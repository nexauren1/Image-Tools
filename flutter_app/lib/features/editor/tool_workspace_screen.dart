import 'dart:convert';
import 'dart:typed_data';

import 'package:cross_file/cross_file.dart';
import 'package:image_picker/image_picker.dart';
import 'package:material_ui/material_ui.dart';
import 'package:share_plus/share_plus.dart';
import 'package:flutter/services.dart';

import '../../core/catalog/tool_catalog.dart';
import '../../core/theme/app_theme.dart';
import '../processing/document_engine.dart';
import '../processing/image_engine.dart';
import '../processing/image_exporter.dart';
import '../processing/image_intelligence.dart';
import '../processing/image_recommendation_engine.dart';
import 'crop_editor_screen.dart';

class ToolWorkspaceScreen extends StatefulWidget {
  const ToolWorkspaceScreen({super.key, required this.tool});

  final ToolDescriptor tool;

  @override
  State<ToolWorkspaceScreen> createState() => _ToolWorkspaceScreenState();
}

class _ToolWorkspaceScreenState extends State<ToolWorkspaceScreen> {
  final ImagePicker _picker = ImagePicker();
  final TextEditingController _widthController = TextEditingController();
  final TextEditingController _heightController = TextEditingController();
  final TextEditingController _watermarkController =
      TextEditingController(text: 'NEXAUREN');

  Uint8List? _imageBytes;
  String? _sourcePath;
  List<Uint8List> _selectedImages = [];
  List<String> _paletteColors = [];
  String? _textResult;
  String _resultExtension = 'png';
  String _resultMimeType = 'image/png';
  int _frameThickness = 28;
  int _pixelBlock = 18;
  double _effectStrength = 0.55;
  double _watermarkOpacity = 0.72;
  String _watermarkPosition = 'bottomRight';
  String _socialPreset = 'square';
  Uint8List? _resultBytes;
  String? _fileName;
  String? _resultLabel;
  ImageDetails? _details;
  bool _loading = false;
  bool _processing = false;
  bool _lockAspect = true;
  int _quality = 88;
  int _quarterTurns = 1;
  bool _flipHorizontal = false;
  bool _flipVertical = false;
  double _brightness = 1;
  double _contrast = 1;
  double _saturation = 1;
  double _filterStrength = 0.5;
  String _format = 'jpeg';
  String _filter = 'grayscale';

  @override
  void dispose() {
    _widthController.dispose();
    _heightController.dispose();
    _watermarkController.dispose();
    super.dispose();
  }

  Future<void> _pickImage(ImageSource source) async {
    setState(() => _loading = true);
    try {
      final file = await _picker.pickImage(source: source, requestFullMetadata: true);
      if (file == null) return;
      final bytes = await file.readAsBytes();
      if (bytes.isEmpty) throw const FormatException('O ficheiro selecionado está vazio.');
      final details = await ImageEngine.inspect(bytes);
      if (!mounted) return;

      setState(() {
        _imageBytes = bytes;
        _sourcePath = file.path;
        _selectedImages = [bytes];
        _resultBytes = null;
        _resultLabel = null;
        _textResult = null;
        _paletteColors = [];
        _fileName = file.name;
        _details = details;
        _widthController.text = details.width.toString();
        _heightController.text = details.height.toString();
      });

      if (widget.tool.id == ToolId.crop) {
        await Navigator.of(context).push<bool>(
          MaterialPageRoute(builder: (_) => CropEditorScreen(originalBytes: bytes)),
        );
      }
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(error.toString())));
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _pickForTool() async {
    if (widget.tool.id == ToolId.collage || widget.tool.id == ToolId.batch) {
      await _pickMultipleImages();
    } else {
      await _pickImage(ImageSource.gallery);
    }
  }

  Future<void> _pickMultipleImages() async {
    setState(() => _loading = true);
    try {
      final files = await _picker.pickMultiImage();
      if (files.isEmpty) return;
      if (widget.tool.id == ToolId.collage && files.length < 2) {
        throw const FormatException('Escolhe pelo menos duas imagens para criar uma colagem.');
      }
      if (files.length > 60) {
        throw const FormatException('O limite de processamento em lote é 60 imagens.');
      }
      final bytesList = <Uint8List>[];
      for (final file in files) {
        final bytes = await file.readAsBytes();
        if (bytes.isNotEmpty) bytesList.add(bytes);
      }
      if (bytesList.isEmpty) {
        throw const FormatException('Não foi possível ler as imagens selecionadas.');
      }
      final details = await ImageEngine.inspect(bytesList.first);
      if (!mounted) return;
      setState(() {
        _selectedImages = bytesList;
        _imageBytes = bytesList.first;
        _sourcePath = files.first.path;
        _fileName = '${files.length} imagens selecionadas';
        _details = details;
        _resultBytes = null;
        _resultLabel = null;
        _textResult = null;
        _paletteColors = [];
        _widthController.text = details.width.toString();
        _heightController.text = details.height.toString();
      });
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(error.toString())),
      );
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  void _changeWidth(String value) {
    if (!_lockAspect || _details == null) return;
    final width = int.tryParse(value);
    if (width == null || width < 1) return;
    final aspect = _details!.width / _details!.height;
    _heightController.text = (width / aspect).round().toString();
  }

  void _changeHeight(String value) {
    if (!_lockAspect || _details == null) return;
    final height = int.tryParse(value);
    if (height == null || height < 1) return;
    final aspect = _details!.width / _details!.height;
    _widthController.text = (height * aspect).round().toString();
  }

  Future<void> _runOperation() async {
    final input = _imageBytes;
    final details = _details;
    if ((input == null || details == null) && _selectedImages.isEmpty) return;
    if (_processing || _loading) return;

    setState(() {
      _processing = true;
      _resultBytes = null;
      _textResult = null;
      _paletteColors = [];
    });

    try {
      final toolId = widget.tool.id;

      if (toolId == ToolId.imageInfo || toolId == ToolId.metadata) {
        setState(() => _resultLabel = toolId == ToolId.imageInfo
            ? 'Informação técnica disponível abaixo.'
            : 'A exportação sem metadados cria uma cópia limpa em PNG.');
        if (toolId == ToolId.metadata && input != null) {
          final cleaned = await ImageEngine.convert(
            input,
            format: ImageOutputFormat.png,
          );
          await _saveImageOutput(cleaned, 'png', 'image/png', 'Cópia sem metadados');
        }
        return;
      }

      if (toolId == ToolId.palette && input != null) {
        final palette = await ImageEngine.extractPalette(input, colorCount: 8);
        if (!mounted) return;
        setState(() {
          _paletteColors = palette;
          _resultLabel = 'Paleta extraída';
        });
        return;
      }

      if (toolId == ToolId.ocr) {
        final sourcePath = _sourcePath;
        if (sourcePath == null) {
          throw const FormatException('Escolhe novamente o ficheiro para executar o OCR.');
        }
        final recognized = await ImageIntelligence.recognizeText(sourcePath);
        if (!mounted) return;
        setState(() {
          _textResult = recognized.isEmpty
              ? 'Não foi encontrado texto legível nesta imagem.'
              : recognized;
          _resultLabel = 'Texto reconhecido';
        });
        return;
      }

      if (toolId == ToolId.base64 && input != null) {
        final encoded = base64Encode(input);
        await Clipboard.setData(ClipboardData(text: encoded));
        if (!mounted) return;
        setState(() {
          _textResult = encoded;
          _resultLabel = 'Base64 copiado para a área de transferência';
        });
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Base64 copiado.')),
        );
        return;
      }

      if (toolId == ToolId.batch) {
        final sourceImages = _selectedImages;
        if (sourceImages.isEmpty) {
          throw const FormatException('Seleciona várias imagens primeiro.');
        }
        final format = _format == 'webp'
            ? ImageOutputFormat.webp
            : ImageOutputFormat.jpeg;
        final outputs = await ImageEngine.batchCompress(
          sourceImages,
          quality: _quality,
          format: format,
        );
        final extension = _extensionFor(format);
        final mime = _mimeFor(format);
        final stamp = DateTime.now().millisecondsSinceEpoch;
        for (var index = 0; index < outputs.length; index++) {
          await ImageExporter.saveToGallery(
            bytes: outputs[index],
            fileName: 'ImageTools_Batch_${stamp}_${index}.$extension',
            mimeType: mime,
          );
        }
        if (!mounted) return;
        setState(() {
          _resultBytes = outputs.first;
          _resultLabel = '${outputs.length} imagens comprimidas e guardadas';
          _resultExtension = extension;
          _resultMimeType = mime;
        });
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('${outputs.length} imagens processadas e guardadas.')),
        );
        return;
      }

      if (toolId == ToolId.collage) {
        final sourceImages = _selectedImages;
        if (sourceImages.length < 2) {
          throw const FormatException('Seleciona de 2 a 9 imagens para criar uma colagem.');
        }
        final output = await ImageEngine.createCollage(sourceImages);
        await _saveImageOutput(output, 'png', 'image/png', 'Colagem criada');
        return;
      }

      if (toolId == ToolId.splitImage) {
        if (input == null) throw const FormatException('Escolhe uma imagem primeiro.');
        final outputs = await ImageEngine.splitIntoGrid(input, rows: 3, columns: 3);
        final stamp = DateTime.now().millisecondsSinceEpoch;
        for (var index = 0; index < outputs.length; index++) {
          await ImageExporter.saveToGallery(
            bytes: outputs[index],
            fileName: 'ImageTools_Split_${stamp}_${index + 1}.png',
            mimeType: 'image/png',
          );
        }
        if (!mounted) return;
        setState(() {
          _resultBytes = outputs.first;
          _resultLabel = '9 segmentos guardados na galeria';
          _resultExtension = 'png';
          _resultMimeType = 'image/png';
        });
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('A imagem foi dividida em 9 partes.')),
        );
        return;
      }

      if (toolId == ToolId.removeBackground) {
        final sourcePath = _sourcePath;
        if (input == null || sourcePath == null) {
          throw const FormatException('Seleciona novamente a imagem antes de remover o fundo.');
        }
        final output = await ImageIntelligence.removeBackground(
          sourcePath,
          input,
        );
        await _saveImageOutput(output, 'png', 'image/png', 'Fundo removido');
        return;
      }

      if (toolId == ToolId.faceBlur) {
        final sourcePath = _sourcePath;
        if (input == null || sourcePath == null) {
          throw const FormatException('Seleciona novamente a imagem antes de detetar rostos.');
        }
        final result = await ImageIntelligence.blurFaces(
          sourcePath,
          input,
          blurStrength: 22,
        );
        if (result.faceCount == 0) {
          throw const FormatException('Não foram detetados rostos nesta imagem.');
        }
        await _saveImageOutput(
          result.bytes,
          'png',
          'image/png',
          '${result.faceCount} rostos desfocados',
        );
        return;
      }

      if (toolId == ToolId.imageToPdf) {
        if (input == null) throw const FormatException('Escolhe uma imagem primeiro.');
        final output = await DocumentEngine.imagesToPdf([input]);
        await _saveDocumentOutput(
          output,
          'ImageTools_${DateTime.now().millisecondsSinceEpoch}.pdf',
        );
        return;
      }

      if (input == null) {
        throw const FormatException('Escolhe uma imagem para começar.');
      }

      late final Uint8List output;
      late final String extension;
      late final String mimeType;
      late final String label;

      switch (toolId) {
        case ToolId.resize:
          final width = int.tryParse(_widthController.text.trim());
          final height = int.tryParse(_heightController.text.trim());
          if (width == null || height == null) {
            throw const FormatException('Introduz uma largura e altura válidas.');
          }
          output = await ImageEngine.resize(input, width: width, height: height);
          extension = 'png';
          mimeType = 'image/png';
          label = 'Imagem redimensionada';
          break;

        case ToolId.compress:
          final format = _format == 'webp'
              ? ImageOutputFormat.webp
              : ImageOutputFormat.jpeg;
          output = await ImageEngine.compress(input, format: format, quality: _quality);
          extension = _extensionFor(format);
          mimeType = _mimeFor(format);
          label = 'Imagem comprimida';
          break;

        case ToolId.convert:
          final format = _outputFormat;
          output = await ImageEngine.convert(input, format: format, quality: _quality);
          extension = _extensionFor(format);
          mimeType = _mimeFor(format);
          label = 'Formato convertido';
          break;

        case ToolId.enhance:
          output = await ImageEngine.enhance(
            input,
            brightness: _brightness,
            contrast: _contrast,
            saturation: _saturation,
          );
          extension = 'png';
          mimeType = 'image/png';
          label = 'Imagem melhorada';
          break;

        case ToolId.filters:
          output = await ImageEngine.applyFilter(
            input,
            filter: _filter,
            strength: _filterStrength,
          );
          extension = 'png';
          mimeType = 'image/png';
          label = 'Efeito aplicado';
          break;

        case ToolId.blur:
          output = await ImageEngine.applyFilter(
            input,
            filter: 'blur',
            strength: _effectStrength,
          );
          extension = 'png';
          mimeType = 'image/png';
          label = 'Desfoque aplicado';
          break;

        case ToolId.sharpen:
          output = await ImageEngine.applyFilter(
            input,
            filter: 'sharpen',
            strength: _effectStrength,
          );
          extension = 'png';
          mimeType = 'image/png';
          label = 'Nitidez aplicada';
          break;

        case ToolId.rotate:
          output = await ImageEngine.rotate(
            input,
            quarterTurns: _quarterTurns,
            flipHorizontal: _flipHorizontal,
            flipVertical: _flipVertical,
          );
          extension = 'png';
          mimeType = 'image/png';
          label = 'Orientação atualizada';
          break;

        case ToolId.addText:
        case ToolId.watermark:
          output = await ImageEngine.addTextWatermark(
            input,
            text: _watermarkController.text,
            opacity: _watermarkOpacity,
            position: _watermarkPosition,
          );
          extension = 'png';
          mimeType = 'image/png';
          label = 'Marca de água aplicada';
          break;

        case ToolId.border:
          output = await ImageEngine.addBorder(
            input,
            thickness: _frameThickness,
            red: _frameColor.$1,
            green: _frameColor.$2,
            blue: _frameColor.$3,
          );
          extension = 'png';
          mimeType = 'image/png';
          label = 'Moldura aplicada';
          break;

        case ToolId.pixelate:
          output = await ImageEngine.pixelate(
            input,
            blockSize: _pixelBlock,
          );
          extension = 'png';
          mimeType = 'image/png';
          label = 'Efeito mosaico aplicado';
          break;

        case ToolId.socialPresets:
          final size = _socialDimensions;
          output = await ImageEngine.fitSocialPreset(
            input,
            width: size.$1,
            height: size.$2,
          );
          extension = 'png';
          mimeType = 'image/png';
          label = 'Formato social preparado';
          break;

        case ToolId.documentScan:
          final gray = await ImageEngine.applyFilter(
            input,
            filter: 'grayscale',
            strength: 1,
          );
          output = await ImageEngine.enhance(
            gray,
            brightness: 1.08,
            contrast: 1.35,
            saturation: 0,
          );
          extension = 'png';
          mimeType = 'image/png';
          label = 'Documento realçado';
          break;

        default:
          throw StateError('Esta ferramenta ainda não foi ativada.');
      }

      await _saveImageOutput(output, extension, mimeType, label);
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(error.toString()),
          backgroundColor: Theme.of(context).colorScheme.error,
        ),
      );
    } finally {
      if (mounted) setState(() => _processing = false);
    }
  }

  Future<void> _saveImageOutput(
    Uint8List output,
    String extension,
    String mimeType,
    String label,
  ) async {
    await ImageExporter.saveToGallery(
      bytes: output,
      fileName: 'ImageTools_${widget.tool.id.name}_${DateTime.now().millisecondsSinceEpoch}.$extension',
      mimeType: mimeType,
    );
    if (!mounted) return;
    setState(() {
      _resultBytes = output;
      _resultLabel = label;
      _resultExtension = extension;
      _resultMimeType = mimeType;
    });
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          '$label • ${_formatBytes(output.length)} • guardada na galeria',
        ),
      ),
    );
  }

  Future<void> _saveDocumentOutput(Uint8List output, String fileName) async {
    final result = await SharePlus.instance.share(
      ShareParams(
        files: [
          XFile.fromData(
            output,
            mimeType: 'application/pdf',
          ),
        ],
        fileNameOverrides: [fileName],
        title: 'Partilhar documento PDF',
        text: 'PDF criado com Image Tools.',
      ),
    );
    if (!mounted) return;
    setState(() {
      _resultBytes = output;
      _resultLabel = 'PDF pronto para partilhar (${_formatBytes(output.length)})';
      _resultExtension = 'pdf';
      _resultMimeType = 'application/pdf';
    });
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          result.status == ShareResultStatus.unavailable
              ? 'PDF criado. Usa o botão de partilha para o enviar.'
              : 'PDF criado e partilhado.',
        ),
      ),
    );
  }

  Future<void> _shareResult() async {
    final bytes = _resultBytes;
    if (bytes == null) return;
    try {
      await SharePlus.instance.share(
        ShareParams(
          files: [
            XFile.fromData(
              bytes,
              mimeType: _resultMimeType,
            ),
          ],
          fileNameOverrides: [
            'ImageTools_${widget.tool.id.name}_result.$_resultExtension',
          ],
          title: 'Partilhar resultado do Image Tools',
          text: 'Editado com Image Tools.',
        ),
      );
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Falha ao partilhar: $error')),
      );
    }
  }

  Future<void> _shareTextResult() async {
    final text = _textResult;
    if (text == null || text.isEmpty) return;
    await SharePlus.instance.share(
      ShareParams(
        text: text,
        title: 'Partilhar resultado do Image Tools',
      ),
    );
  }

  (int, int, int) get _frameColor => switch (_format) {
        'black' => (18, 20, 28),
        'blue' => (22, 140, 255),
        'violet' => (139, 92, 246),
        _ => (255, 255, 255),
      };

  (int, int) get _socialDimensions => switch (_socialPreset) {
        'story' => (1080, 1920),
        'landscape' => (1200, 628),
        'video' => (1280, 720),
        'profile' => (1080, 1080),
        _ => (1080, 1080),
      };

  ImageOutputFormat get _outputFormat {
    switch (_format) {
      case 'png':
        return ImageOutputFormat.png;
      case 'webp':
        return ImageOutputFormat.webp;
      default:
        return ImageOutputFormat.jpeg;
    }
  }

  String _extensionFor(ImageOutputFormat format) => switch (format) {
        ImageOutputFormat.jpeg => 'jpg',
        ImageOutputFormat.png => 'png',
        ImageOutputFormat.webp => 'webp',
      };

  String _mimeFor(ImageOutputFormat format) => switch (format) {
        ImageOutputFormat.jpeg => 'image/jpeg',
        ImageOutputFormat.png => 'image/png',
        ImageOutputFormat.webp => 'image/webp',
      };

  String _formatBytes(int bytes) {
    if (bytes < 1024) return '$bytes B';
    if (bytes < 1024 * 1024) return '${(bytes / 1024).toStringAsFixed(1)} KB';
    return '${(bytes / (1024 * 1024)).toStringAsFixed(2)} MB';
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.tool.title),
        actions: [
          if (_imageBytes != null)
            IconButton(
              tooltip: 'Escolher outra imagem',
              onPressed: _loading || _processing ? null : () => _pickImage(ImageSource.gallery),
              icon: const Icon(Icons.swap_horiz_rounded),
            ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
        children: [
          _toolHeader(context),
          const SizedBox(height: 18),
          if (_imageBytes == null) ...[
            _emptyImageCard(context),
            const SizedBox(height: 16),
            FilledButton.icon(
              onPressed: _loading ? null : _pickForTool,
              icon: _loading
                  ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                  : const Icon(Icons.photo_library_outlined),
              label: Text(
                _loading
                    ? 'A abrir a galeria…'
                    : (widget.tool.id == ToolId.collage || widget.tool.id == ToolId.batch
                        ? 'Selecionar várias imagens'
                        : 'Selecionar da galeria'),
              ),
            ),
            if (widget.tool.id != ToolId.collage && widget.tool.id != ToolId.batch) ...[
              const SizedBox(height: 10),
              OutlinedButton.icon(
                onPressed: _loading ? null : () => _pickImage(ImageSource.camera),
                icon: const Icon(Icons.camera_alt_outlined),
                label: const Text('Tirar fotografia'),
              ),
            ],
            if (_selectedImages.length > 1) ...[
              const SizedBox(height: 10),
              Text(
                _selectedImages.length.toString() + ' imagens selecionadas',
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.bodySmall,
              ),
            ],
          ] else ...[
            _sourcePreview(context),
            const SizedBox(height: 14),
            if (_details != null) _recommendationPanel(context),
            const SizedBox(height: 14),
            _operationControls(context),
            const SizedBox(height: 18),
            if (widget.tool.id != ToolId.imageInfo && widget.tool.id != ToolId.crop) FilledButton.icon(
              onPressed: _processing ? null : _runOperation,
              icon: _processing
                  ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                  : const Icon(Icons.bolt_rounded),
              label: Text(_processing ? 'A processar no dispositivo…' : _actionLabel),
            ),
            if (_paletteColors.isNotEmpty) ...[
              const SizedBox(height: 14),
              _palettePreview(context),
            ],
            if (_textResult != null) ...[
              const SizedBox(height: 14),
              _textResultPanel(context),
            ],
            if (_resultBytes != null) ...[
              const SizedBox(height: 18),
              _resultPreview(context),
            ],
          ],
        ],
      ),
    );
  }

  String get _actionLabel => switch (widget.tool.id) {
        ToolId.resize => 'Redimensionar e guardar',
        ToolId.compress => 'Comprimir e guardar',
        ToolId.convert => 'Converter e guardar',
        ToolId.enhance => 'Melhorar e guardar',
        ToolId.filters => 'Aplicar efeito e guardar',
        ToolId.rotate => 'Rodar e guardar',
        ToolId.addText => 'Adicionar texto e guardar',
        ToolId.watermark => 'Aplicar marca de água e guardar',
        ToolId.border => 'Adicionar moldura e guardar',
        ToolId.pixelate => 'Pixelizar e guardar',
        ToolId.socialPresets => 'Preparar formato social e guardar',
        ToolId.blur => 'Aplicar desfoque e guardar',
        ToolId.sharpen => 'Melhorar nitidez e guardar',
        ToolId.collage => 'Criar colagem e guardar',
        ToolId.batch => 'Comprimir e guardar todas',
        ToolId.imageToPdf => 'Criar PDF e partilhar',
        ToolId.removeBackground => 'Remover fundo e guardar',
        ToolId.faceBlur => 'Desfocar rostos e guardar',
        ToolId.ocr => 'Extrair texto',
        ToolId.palette => 'Extrair paleta de cores',
        ToolId.base64 => 'Gerar e copiar Base64',
        ToolId.metadata => 'Criar cópia sem metadados',
        ToolId.splitImage => 'Dividir em 9 imagens',
        ToolId.documentScan => 'Melhorar documento',
        _ => 'Executar ferramenta',
      };

  Widget _toolHeader(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(19),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(23),
        gradient: LinearGradient(
          colors: [widget.tool.color, widget.tool.color.withValues(alpha: 0.55), const Color(0xFF111C46)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
      ),
      child: Row(
        children: [
          Icon(widget.tool.icon, color: Colors.white, size: 34),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(widget.tool.title, style: const TextStyle(color: Colors.white, fontSize: 19, fontWeight: FontWeight.w900)),
                const SizedBox(height: 4),
                Text(widget.tool.subtitle, style: const TextStyle(color: Colors.white70)),
                const SizedBox(height: 7),
                const Text('Processamento local', style: TextStyle(color: Colors.white, fontSize: 11, fontWeight: FontWeight.w700)),
              ],
            ),
          ),
          if (widget.tool.premium)
            const Icon(Icons.workspace_premium, color: Color(0xFFFFD54F), size: 26),
        ],
      ),
    );
  }

  Widget _emptyImageCard(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Container(
      height: 230,
      decoration: BoxDecoration(
        color: colors.surfaceContainerHighest,
        borderRadius: BorderRadius.circular(22),
        border: Border.all(color: colors.outlineVariant.withValues(alpha: 0.5)),
      ),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.add_photo_alternate_outlined, size: 54, color: colors.primary),
          const SizedBox(height: 12),
          const Text('Escolha uma imagem para começar', style: TextStyle(fontWeight: FontWeight.w700)),
          const SizedBox(height: 6),
          Text('O ficheiro permanece no seu dispositivo.', style: Theme.of(context).textTheme.bodySmall),
        ],
      ),
    );
  }

  Widget _sourcePreview(BuildContext context) {
    final details = _details!;
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surfaceContainer,
        borderRadius: BorderRadius.circular(22),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          ClipRRect(
            borderRadius: BorderRadius.circular(15),
            child: Image.memory(_imageBytes!, height: 250, width: double.infinity, fit: BoxFit.contain, cacheWidth: 1400),
          ),
          const SizedBox(height: 12),
          Text(_fileName ?? 'Imagem selecionada', maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontWeight: FontWeight.w800)),
          const SizedBox(height: 4),
          Text('${details.width} × ${details.height} px  •  ${details.format}  •  ${_formatBytes(details.bytes)}',
              style: Theme.of(context).textTheme.bodySmall),
        ],
      ),
    );
  }

  Widget _operationControls(BuildContext context) {
    switch (widget.tool.id) {
      case ToolId.resize:
        return _resizeControls(context);
      case ToolId.compress:
        return _compressControls(context);
      case ToolId.convert:
        return _convertControls(context);
      case ToolId.enhance:
        return _enhanceControls(context);
      case ToolId.filters:
        return _filterControls(context);
      case ToolId.rotate:
        return _rotateControls(context);
      case ToolId.blur:
      case ToolId.sharpen:
        return _effectControls(context);
      case ToolId.addText:
      case ToolId.watermark:
        return _watermarkControls(context);
      case ToolId.border:
        return _borderControls(context);
      case ToolId.pixelate:
        return _pixelateControls(context);
      case ToolId.socialPresets:
        return _socialControls(context);
      case ToolId.collage:
      case ToolId.batch:
        return _multiImageControls(context);
      case ToolId.imageInfo:
      case ToolId.metadata:
        return _infoControls(context);
      case ToolId.palette:
        return _messagePanel(
          'Paleta de cores',
          'Extrai até oito cores dominantes da imagem e copia os respetivos códigos HEX.',
        );
      case ToolId.ocr:
        return _messagePanel(
          'Reconhecimento de texto',
          'O OCR corre no dispositivo através do Google ML Kit. A primeira utilização pode precisar de transferir o modelo.',
        );
      case ToolId.removeBackground:
        return _messagePanel(
          'Remover fundo',
          'A segmentação tenta separar o primeiro plano do fundo e exporta um PNG com transparência. O resultado depende da imagem e do modelo instalado.',
        );
      case ToolId.faceBlur:
        return _messagePanel(
          'Privacidade de rostos',
          'Deteta rostos e desfoca as regiões encontradas antes de guardar uma cópia.',
        );
      case ToolId.imageToPdf:
        return _messagePanel(
          'Imagem para PDF',
          'Cria um documento PDF a partir da imagem selecionada. O ficheiro abre o menu de partilha do Android.',
        );
      case ToolId.base64:
        return _messagePanel(
          'Imagem para Base64',
          'Converte os bytes da imagem para Base64 e copia o resultado para a área de transferência.',
        );
      case ToolId.splitImage:
        return _messagePanel(
          'Dividir imagem',
          'Divide a imagem em nove partes iguais e guarda cada segmento na galeria.',
        );
      case ToolId.documentScan:
        return _messagePanel(
          'Digitalizar documento',
          'Aplica tons de cinzento, brilho e contraste para aumentar a legibilidade do papel.',
        );
      case ToolId.crop:
        return _messagePanel(
          'Editor de recorte',
          'O editor de recorte abre automaticamente quando selecionas a imagem.',
        );
      default:
        return _pendingControls(context);
    }
  }

  Widget _panel(BuildContext context, String title, Widget child) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surfaceContainer,
        borderRadius: BorderRadius.circular(20),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 15)),
          const SizedBox(height: 14),
          child,
        ],
      ),
    );
  }

  Widget _resizeControls(BuildContext context) {
    return _panel(
      context,
      'Dimensões de saída',
      Column(
        children: [
          Row(
            children: [
              Expanded(child: _dimensionField('Largura (px)', _widthController, _changeWidth)),
              const SizedBox(width: 12),
              Expanded(child: _dimensionField('Altura (px)', _heightController, _changeHeight)),
            ],
          ),
          SwitchListTile(
            contentPadding: EdgeInsets.zero,
            value: _lockAspect,
            onChanged: (value) => setState(() => _lockAspect = value),
            title: const Text('Manter proporção original'),
          ),
          Text('A imagem original não será alterada. O novo tamanho só será aplicado ao guardar.',
              style: Theme.of(context).textTheme.bodySmall),
        ],
      ),
    );
  }

  Widget _dimensionField(String label, TextEditingController controller, ValueChanged<String> onChanged) {
    return TextField(
      controller: controller,
      keyboardType: TextInputType.number,
      onChanged: onChanged,
      decoration: InputDecoration(labelText: label, suffixText: 'px'),
    );
  }

  Widget _compressControls(BuildContext context) {
    return _panel(
      context,
      'Qualidade e formato',
      Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _formatChips(['jpeg', 'webp']),
          const SizedBox(height: 14),
          Row(
            children: [
              const Expanded(child: Text('Qualidade', style: TextStyle(fontWeight: FontWeight.w700))),
              Text('$_quality%', style: const TextStyle(fontWeight: FontWeight.w900, color: AppTheme.cyan)),
            ],
          ),
          Slider(
            value: _quality.toDouble(),
            min: 10,
            max: 100,
            divisions: 18,
            label: '$_quality%',
            onChanged: (value) => setState(() => _quality = value.round()),
          ),
          Text('Qualidade mais baixa gera ficheiros menores. A compressão JPEG/WebP é com perdas.',
              style: Theme.of(context).textTheme.bodySmall),
        ],
      ),
    );
  }

  Widget _convertControls(BuildContext context) {
    return _panel(
      context,
      'Formato de exportação',
      Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _formatChips(['jpeg', 'png', 'webp']),
          if (_format != 'png') ...[
            const SizedBox(height: 14),
            Row(
              children: [
                const Expanded(child: Text('Qualidade', style: TextStyle(fontWeight: FontWeight.w700))),
                Text('$_quality%', style: const TextStyle(fontWeight: FontWeight.w900, color: AppTheme.cyan)),
              ],
            ),
            Slider(
              value: _quality.toDouble(),
              min: 10,
              max: 100,
              divisions: 18,
              label: '$_quality%',
              onChanged: (value) => setState(() => _quality = value.round()),
            ),
          ],
        ],
      ),
    );
  }

  Widget _formatChips(List<String> formats) {
    return Wrap(
      spacing: 8,
      children: formats.map((format) {
        final selected = _format == format;
        return ChoiceChip(
          label: Text(format.toUpperCase()),
          selected: selected,
          onSelected: (_) => setState(() => _format = format),
        );
      }).toList(),
    );
  }

  Widget _enhanceControls(BuildContext context) {
    return _panel(
      context,
      'Ajustes da imagem',
      Column(
        children: [
          _adjustmentSlider('Brilho', _brightness, (value) => setState(() => _brightness = value)),
          _adjustmentSlider('Contraste', _contrast, (value) => setState(() => _contrast = value)),
          _adjustmentSlider('Saturação', _saturation, (value) => setState(() => _saturation = value)),
          Align(
            alignment: Alignment.centerRight,
            child: TextButton(
              onPressed: () => setState(() {
                _brightness = 1;
                _contrast = 1;
                _saturation = 1;
              }),
              child: const Text('Repor ajustes'),
            ),
          ),
        ],
      ),
    );
  }

  Widget _adjustmentSlider(String label, double value, ValueChanged<double> onChanged) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Expanded(child: Text(label, style: const TextStyle(fontWeight: FontWeight.w700))),
            Text(value.toStringAsFixed(2)),
          ],
        ),
        Slider(value: value, min: 0, max: 2, divisions: 40, onChanged: onChanged),
      ],
    );
  }

  Widget _rotateControls(BuildContext context) {
    return _panel(
      context,
      'Orientação da imagem',
      Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Escolhe o ângulo de rotação e, se necessário, espelha a imagem.',
          ),
          const SizedBox(height: 12),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              for (final turns in [1, 2, 3])
                ChoiceChip(
                  label: Text('${turns * 90}°'),
                  selected: _quarterTurns == turns,
                  onSelected: (_) => setState(() => _quarterTurns = turns),
                ),
            ],
          ),
          const SizedBox(height: 10),
          SwitchListTile(
            contentPadding: EdgeInsets.zero,
            title: const Text('Espelhar horizontalmente'),
            value: _flipHorizontal,
            onChanged: (value) =>
                setState(() => _flipHorizontal = value),
          ),
          SwitchListTile(
            contentPadding: EdgeInsets.zero,
            title: const Text('Espelhar verticalmente'),
            value: _flipVertical,
            onChanged: (value) => setState(() => _flipVertical = value),
          ),
          Align(
            alignment: Alignment.centerRight,
            child: TextButton.icon(
              onPressed: () => setState(() {
                _quarterTurns = 0;
                _flipHorizontal = false;
                _flipVertical = false;
              }),
              icon: const Icon(Icons.restart_alt_rounded),
              label: const Text('Repor orientação'),
            ),
          ),
        ],
      ),
    );
  }

  Widget _filterControls(BuildContext context) {
    const filters = {
      'grayscale': 'Preto e branco',
      'sepia': 'Sépia',
      'negative': 'Negativo',
      'bright': 'Brilho',
      'contrast': 'Contraste',
      'saturate': 'Vivo',
      'blur': 'Desfoque',
      'sharpen': 'Nitidez',
    };
    return _panel(
      context,
      'Filtros e efeitos',
      Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: filters.entries.map((entry) => ChoiceChip(
              label: Text(entry.value),
              selected: _filter == entry.key,
              onSelected: (_) => setState(() => _filter = entry.key),
            )).toList(),
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              const Expanded(child: Text('Intensidade', style: TextStyle(fontWeight: FontWeight.w700))),
              Text('${(_filterStrength * 100).round()}%'),
            ],
          ),
          Slider(
            value: _filterStrength,
            min: 0,
            max: 1,
            divisions: 20,
            onChanged: (value) => setState(() => _filterStrength = value),
          ),
        ],
      ),
    );
  }

  Widget _effectControls(BuildContext context) {
    final isBlur = widget.tool.id == ToolId.blur;
    return _panel(
      context,
      isBlur ? 'Intensidade do desfoque' : 'Nível de nitidez',
      Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('${(_effectStrength * 100).round()}%'),
          Slider(
            value: _effectStrength,
            min: 0.1,
            max: 1,
            divisions: 18,
            onChanged: (value) => setState(() => _effectStrength = value),
          ),
          Text(
            isBlur
                ? 'Usa um desfoque progressivo para suavizar detalhes.'
                : 'Realça os contornos com um filtro de nitidez.',
            style: Theme.of(context).textTheme.bodySmall,
          ),
        ],
      ),
    );
  }

  Widget _watermarkControls(BuildContext context) {
    return _panel(
      context,
      widget.tool.id == ToolId.addText ? 'Texto sobre a imagem' : 'Marca de água',
      Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          TextField(
            controller: _watermarkController,
            maxLength: 80,
            decoration: const InputDecoration(
              labelText: 'Texto',
              hintText: 'Escreve o texto a inserir',
            ),
          ),
          const SizedBox(height: 8),
          Text('Opacidade: ${(_watermarkOpacity * 100).round()}%'),
          Slider(
            value: _watermarkOpacity,
            min: 0.12,
            max: 1,
            onChanged: (value) => setState(() => _watermarkOpacity = value),
          ),
          const Text('Posição'),
          Wrap(
            spacing: 7,
            runSpacing: 7,
            children: [
              for (final option in [
                'topLeft',
                'topRight',
                'center',
                'bottomLeft',
                'bottomRight',
              ])
                ChoiceChip(
                  label: Text(
                    switch (option) {
                      'topLeft' => 'Superior esquerda',
                      'topRight' => 'Superior direita',
                      'center' => 'Centro',
                      'bottomLeft' => 'Inferior esquerda',
                      _ => 'Inferior direita',
                    },
                  ),
                  selected: _watermarkPosition == option,
                  onSelected: (_) =>
                      setState(() => _watermarkPosition = option),
                ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _borderControls(BuildContext context) {
    return _panel(
      context,
      'Moldura da imagem',
      Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Espessura: $_frameThickness px'),
          Slider(
            value: _frameThickness.toDouble(),
            min: 2,
            max: 120,
            divisions: 59,
            onChanged: (value) =>
                setState(() => _frameThickness = value.round()),
          ),
          const Text('Cor da moldura'),
          _formatChips(['white', 'black', 'blue', 'violet']),
        ],
      ),
    );
  }

  Widget _pixelateControls(BuildContext context) {
    return _panel(
      context,
      'Efeito mosaico',
      Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Tamanho dos blocos: $_pixelBlock px'),
          Slider(
            value: _pixelBlock.toDouble(),
            min: 3,
            max: 96,
            divisions: 31,
            onChanged: (value) => setState(() => _pixelBlock = value.round()),
          ),
        ],
      ),
    );
  }

  Widget _socialControls(BuildContext context) {
    const choices = {
      'square': 'Quadrado 1:1',
      'story': 'Story 9:16',
      'landscape': 'Publicação 1.91:1',
      'video': 'Vídeo 16:9',
      'profile': 'Perfil 1:1',
    };
    return _panel(
      context,
      'Tamanho para redes sociais',
      Wrap(
        spacing: 8,
        runSpacing: 8,
        children: choices.entries.map((entry) {
          return ChoiceChip(
            label: Text(entry.value),
            selected: _socialPreset == entry.key,
            onSelected: (_) => setState(() => _socialPreset = entry.key),
          );
        }).toList(),
      ),
    );
  }

  Widget _multiImageControls(BuildContext context) {
    return _panel(
      context,
      widget.tool.id == ToolId.collage ? 'Composição da colagem' : 'Compressão em lote',
      Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '${_selectedImages.length} imagem(ns) selecionada(s)',
            style: const TextStyle(fontWeight: FontWeight.w800),
          ),
          const SizedBox(height: 8),
          if (widget.tool.id == ToolId.batch) ...[
            _formatChips(['jpeg', 'webp']),
            const SizedBox(height: 10),
            Text('Qualidade: $_quality%'),
            Slider(
              value: _quality.toDouble(),
              min: 10,
              max: 100,
              divisions: 18,
              onChanged: (value) => setState(() => _quality = value.round()),
            ),
          ] else
            const Text(
              'Escolhe entre 2 e 9 imagens. O estúdio organiza-as automaticamente numa grelha.',
            ),
          TextButton.icon(
            onPressed: _loading || _processing ? null : _pickMultipleImages,
            icon: const Icon(Icons.add_photo_alternate_outlined),
            label: const Text('Escolher imagens'),
          ),
        ],
      ),
    );
  }

  Widget _messagePanel(String title, String message) {
    return _panel(
      context,
      title,
      Text(message, style: Theme.of(context).textTheme.bodyMedium),
    );
  }

  Widget _recommendationPanel(BuildContext context) {
    final details = _details;
    if (details == null) return const SizedBox.shrink();
    final recommendations = ImageRecommendationEngine.forImage(details);
    return _panel(
      context,
      'Recomendações para esta imagem',
      Column(
        children: recommendations.map((recommendation) {
          final tool = ToolCatalog.all.firstWhere(
            (item) => item.id.name == recommendation.toolId,
            orElse: () => ToolCatalog.primary.first,
          );
          return Padding(
            padding: const EdgeInsets.only(bottom: 9),
            child: Material(
              color: Theme.of(context).colorScheme.surfaceContainerHighest,
              borderRadius: BorderRadius.circular(15),
              child: InkWell(
                borderRadius: BorderRadius.circular(15),
                onTap: () => Navigator.of(context).push(
                  MaterialPageRoute<void>(
                    builder: (_) => ToolWorkspaceScreen(tool: tool),
                  ),
                ),
                child: Padding(
                  padding: const EdgeInsets.all(12),
                  child: Row(
                    children: [
                      Container(
                        width: 38,
                        height: 38,
                        decoration: BoxDecoration(
                          color: tool.color,
                          borderRadius: BorderRadius.circular(12),
                        ),
                        child: Icon(tool.icon, color: Colors.white, size: 20),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              recommendation.title,
                              style: const TextStyle(fontWeight: FontWeight.w800),
                            ),
                            const SizedBox(height: 3),
                            Text(
                              recommendation.description,
                              style: Theme.of(context).textTheme.bodySmall,
                            ),
                          ],
                        ),
                      ),
                      const Icon(Icons.arrow_forward_ios_rounded, size: 14),
                    ],
                  ),
                ),
              ),
            ),
          );
        }).toList(),
      ),
    );
  }

  Widget _palettePreview(BuildContext context) {
    return _panel(
      context,
      'Cores dominantes',
      Wrap(
        spacing: 9,
        runSpacing: 9,
        children: _paletteColors.map((hex) {
          final value = int.tryParse(hex.substring(1), radix: 16) ?? 0;
          return InkWell(
            borderRadius: BorderRadius.circular(12),
            onTap: () => Clipboard.setData(ClipboardData(text: hex)),
            child: Container(
              width: 78,
              padding: const EdgeInsets.all(7),
              decoration: BoxDecoration(
                color: Theme.of(context).colorScheme.surfaceContainerHighest,
                borderRadius: BorderRadius.circular(12),
              ),
              child: Column(
                children: [
                  Container(
                    height: 32,
                    decoration: BoxDecoration(
                      color: Color(0xFF000000 | value),
                      borderRadius: BorderRadius.circular(8),
                    ),
                  ),
                  const SizedBox(height: 5),
                  Text(hex, style: const TextStyle(fontSize: 10)),
                ],
              ),
            ),
          );
        }).toList(),
      ),
    );
  }

  Widget _textResultPanel(BuildContext context) {
    final text = _textResult ?? '';
    return _panel(
      context,
      _resultLabel ?? 'Resultado de texto',
      Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SelectableText(text, maxLines: 8),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            children: [
              OutlinedButton.icon(
                onPressed: () => Clipboard.setData(ClipboardData(text: text)),
                icon: const Icon(Icons.copy_rounded),
                label: const Text('Copiar'),
              ),
              OutlinedButton.icon(
                onPressed: _shareTextResult,
                icon: const Icon(Icons.share_outlined),
                label: const Text('Partilhar'),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _infoControls(BuildContext context) {
    final info = _details!;
    return _panel(
      context,
      'Informação técnica',
      Column(
        children: [
          _infoRow('Resolução', '${info.width} × ${info.height} px'),
          _infoRow('Formato', info.format),
          _infoRow('Tamanho do ficheiro', _formatBytes(info.bytes)),
          _infoRow('Transparência', info.hasAlpha ? 'Suportada' : 'Não detetada'),
        ],
      ),
    );
  }

  Widget _infoRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 7),
      child: Row(
        children: [
          Expanded(child: Text(label)),
          Text(value, style: const TextStyle(fontWeight: FontWeight.w800)),
        ],
      ),
    );
  }

  Widget _pendingControls(BuildContext context) {
    return _panel(
      context,
      'Ferramenta selecionada',
      Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('A interface e o fluxo de importação estão preparados para esta ferramenta. O respetivo motor ainda está a ser migrado e será ativado depois de passar os testes de saída.'),
          const SizedBox(height: 10),
          Text('A ferramenta não será apresentada como concluída antes de o processamento e a exportação estarem verificados.',
              style: Theme.of(context).textTheme.bodySmall),
        ],
      ),
    );
  }

  Widget _resultPreview(BuildContext context) {
    final isPdf = _resultMimeType == 'application/pdf';
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surfaceContainer,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(
          color: AppTheme.electricBlue.withValues(alpha: 0.20),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(
                isPdf ? Icons.picture_as_pdf_rounded : Icons.check_circle_rounded,
                color: isPdf ? const Color(0xFFEF4444) : AppTheme.cyan,
              ),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  _resultLabel ?? 'Resultado',
                  style: const TextStyle(fontWeight: FontWeight.w900),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          if (isPdf)
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(24),
              decoration: BoxDecoration(
                color: Theme.of(context).colorScheme.surfaceContainerHighest,
                borderRadius: BorderRadius.circular(15),
              ),
              child: const Column(
                children: [
                  Icon(Icons.picture_as_pdf_rounded, size: 58, color: Color(0xFFEF4444)),
                  SizedBox(height: 8),
                  Text('Documento PDF pronto', style: TextStyle(fontWeight: FontWeight.w800)),
                ],
              ),
            )
          else
            ClipRRect(
              borderRadius: BorderRadius.circular(15),
              child: Image.memory(
                _resultBytes!,
                height: 220,
                width: double.infinity,
                fit: BoxFit.contain,
                cacheWidth: 1400,
              ),
            ),
          const SizedBox(height: 9),
          Text(
            'Ficheiro: ${_formatBytes(_resultBytes!.length)}',
            style: Theme.of(context).textTheme.bodySmall,
          ),
          const SizedBox(height: 8),
          FilledButton.icon(
            onPressed: _shareResult,
            icon: const Icon(Icons.share_rounded),
            label: const Text('Partilhar resultado'),
          ),
        ],
      ),
    );
  }

}
