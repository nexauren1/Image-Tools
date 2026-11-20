import 'dart:typed_data';

import 'package:image_picker/image_picker.dart';
import 'package:flutter/material.dart';

import '../../core/catalog/tool_catalog.dart';
import '../../core/theme/app_theme.dart';
import '../processing/image_engine.dart';
import '../processing/image_exporter.dart';
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

  Uint8List? _imageBytes;
  Uint8List? _resultBytes;
  String? _fileName;
  String? _resultLabel;
  ImageDetails? _details;
  bool _loading = false;
  bool _processing = false;
  bool _lockAspect = true;
  int _quality = 88;
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
        _resultBytes = null;
        _resultLabel = null;
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
    if (input == null || details == null || _processing) return;

    setState(() => _processing = true);
    try {
      late final Uint8List output;
      late final String extension;
      late final String mimeType;
      late final String label;

      switch (widget.tool.id) {
        case ToolId.resize:
          final width = int.tryParse(_widthController.text.trim());
          final height = int.tryParse(_heightController.text.trim());
          if (width == null || height == null) {
            throw const FormatException('Introduza uma largura e uma altura válidas.');
          }
          output = await ImageEngine.resize(input, width: width, height: height);
          extension = 'png';
          mimeType = 'image/png';
          label = 'Imagem redimensionada';
          break;
        case ToolId.compress:
          final format = _format == 'webp' ? ImageOutputFormat.webp : ImageOutputFormat.jpeg;
          output = await ImageEngine.compress(input, format: format, quality: _quality);
          extension = _format == 'webp' ? 'webp' : 'jpg';
          mimeType = _format == 'webp' ? 'image/webp' : 'image/jpeg';
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
        case ToolId.rotate:
          output = await ImageEngine.rotate(input, quarterTurns: 1);
          extension = 'png';
          mimeType = 'image/png';
          label = 'Imagem rodada';
          break;
        default:
          throw StateError('O motor desta ferramenta ainda está a ser integrado.');
      }

      final savedUri = await ImageExporter.saveToGallery(
        bytes: output,
        fileName: 'ImageTools_${widget.tool.id.name}_${DateTime.now().millisecondsSinceEpoch}.$extension',
        mimeType: mimeType,
      );

      if (!mounted) return;
      setState(() {
        _resultBytes = output;
        _resultLabel = label;
      });
      final savedSize = _formatBytes(output.length);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('$label • $savedSize • guardada na galeria')),
      );
      debugPrint('Image Tools export URI: $savedUri');
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(error.toString()), backgroundColor: Theme.of(context).colorScheme.error),
      );
    } finally {
      if (mounted) setState(() => _processing = false);
    }
  }

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
              onPressed: _loading ? null : () => _pickImage(ImageSource.gallery),
              icon: _loading
                  ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                  : const Icon(Icons.photo_library_outlined),
              label: Text(_loading ? 'A abrir a galeria…' : 'Selecionar da galeria'),
            ),
            const SizedBox(height: 10),
            OutlinedButton.icon(
              onPressed: _loading ? null : () => _pickImage(ImageSource.camera),
              icon: const Icon(Icons.camera_alt_outlined),
              label: const Text('Tirar fotografia'),
            ),
          ] else ...[
            _sourcePreview(context),
            const SizedBox(height: 16),
            _operationControls(context),
            const SizedBox(height: 18),
            if (widget.tool.id != ToolId.imageInfo) FilledButton.icon(
              onPressed: _processing ? null : _runOperation,
              icon: _processing
                  ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                  : const Icon(Icons.bolt_rounded),
              label: Text(_processing ? 'A processar no dispositivo…' : _actionLabel),
            ),
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
      case ToolId.imageInfo:
        return _infoControls(context);
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
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surfaceContainer,
        borderRadius: BorderRadius.circular(20),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(_resultLabel ?? 'Resultado', style: const TextStyle(fontWeight: FontWeight.w900)),
          const SizedBox(height: 10),
          ClipRRect(
            borderRadius: BorderRadius.circular(15),
            child: Image.memory(_resultBytes!, height: 220, width: double.infinity, fit: BoxFit.contain, cacheWidth: 1400),
          ),
          const SizedBox(height: 8),
          Text('Resultado: ${_formatBytes(_resultBytes!.length)}', style: Theme.of(context).textTheme.bodySmall),
        ],
      ),
    );
  }
}
