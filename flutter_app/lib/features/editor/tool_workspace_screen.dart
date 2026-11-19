import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';

import '../../core/catalog/tool_catalog.dart';
import 'crop_editor_screen.dart';

class ToolWorkspaceScreen extends StatefulWidget {
  const ToolWorkspaceScreen({super.key, required this.tool});

  final ToolDescriptor tool;

  @override
  State<ToolWorkspaceScreen> createState() => _ToolWorkspaceScreenState();
}

class _ToolWorkspaceScreenState extends State<ToolWorkspaceScreen> {
  final ImagePicker _picker = ImagePicker();
  Uint8List? _imageBytes;
  String? _fileName;
  bool _loading = false;

  Future<void> _pickImage(ImageSource source) async {
    setState(() => _loading = true);
    try {
      final file = await _picker.pickImage(source: source, requestFullMetadata: true);
      if (file == null) return;
      final bytes = await file.readAsBytes();
      if (bytes.isEmpty) throw const FormatException('O ficheiro selecionado está vazio.');
      if (!mounted) return;

      if (widget.tool.id == ToolId.crop) {
        await Navigator.of(context).push<bool>(
          MaterialPageRoute(builder: (_) => CropEditorScreen(originalBytes: bytes)),
        );
      } else {
        setState(() {
          _imageBytes = bytes;
          _fileName = file.name;
        });
      }
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(error.toString())));
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(title: Text(widget.tool.title)),
      body: ListView(
        padding: const EdgeInsets.all(18),
        children: [
          Container(
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(24),
              gradient: LinearGradient(
                colors: [widget.tool.color.withValues(alpha: 0.95), widget.tool.color.withValues(alpha: 0.48)],
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
                      Text(widget.tool.title, style: const TextStyle(color: Colors.white, fontSize: 19, fontWeight: FontWeight.w800)),
                      const SizedBox(height: 4),
                      Text(widget.tool.subtitle, style: const TextStyle(color: Colors.white70)),
                    ],
                  ),
                ),
                if (widget.tool.premium)
                  const Icon(Icons.workspace_premium, color: Color(0xFFFFD54F), size: 26),
              ],
            ),
          ),
          const SizedBox(height: 22),
          if (_imageBytes == null) ...[
            Container(
              height: 230,
              decoration: BoxDecoration(
                color: colors.surfaceContainerHighest,
                borderRadius: BorderRadius.circular(22),
                border: Border.all(color: colors.outlineVariant.withValues(alpha: 0.4)),
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
            ),
          ] else ...[
            ClipRRect(
              borderRadius: BorderRadius.circular(20),
              child: Image.memory(_imageBytes!, fit: BoxFit.contain, height: 280),
            ),
            const SizedBox(height: 10),
            Text(_fileName ?? 'Imagem selecionada', textAlign: TextAlign.center),
          ],
          const SizedBox(height: 20),
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
          if (_imageBytes != null) ...[
            const SizedBox(height: 18),
            _toolStatus(context, widget.tool.id),
          ],
        ],
      ),
    );
  }

  Widget _toolStatus(BuildContext context, ToolId id) {
    final supported = id == ToolId.crop;
    final message = supported
        ? 'Editor de recorte táctil disponível.'
        : 'Esta ferramenta já está catalogada na nova interface. O motor dedicado será ligado antes da versão de publicação.';
    return Container(
      padding: const EdgeInsets.all(15),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surfaceContainerHighest,
        borderRadius: BorderRadius.circular(16),
      ),
      child: Row(
        children: [
          Icon(supported ? Icons.check_circle_outline : Icons.construction_outlined),
          const SizedBox(width: 10),
          Expanded(child: Text(message)),
        ],
      ),
    );
  }
}
