import 'dart:typed_data';

import 'package:flutter/material.dart';

import '../../core/theme/app_theme.dart';
import '../processing/image_exporter.dart';
import '../processing/image_engine.dart';

enum _CropHandle { move, left, right, top, bottom, topLeft, topRight, bottomLeft, bottomRight }

class CropEditorScreen extends StatefulWidget {
  const CropEditorScreen({super.key, required this.originalBytes});

  final Uint8List originalBytes;

  @override
  State<CropEditorScreen> createState() => _CropEditorScreenState();
}

class _CropEditorScreenState extends State<CropEditorScreen> {
  Rect _crop = const Rect.fromLTWH(0.08, 0.08, 0.84, 0.84);
  String _ratioLabel = 'Livre';
  double? _lockedRatio;
  Size? _previewSize;
  Offset? _dragStart;
  Rect? _startRect;
  _CropHandle _handle = _CropHandle.move;
  bool _processing = false;
  late Future<Size> _imageSizeFuture;

  @override
  void initState() {
    super.initState();
    _imageSizeFuture = _readSize();
  }

  Future<Size> _readSize() async {
    final image = await ImageEngine.decodeForPreview(widget.originalBytes);
    return Size(image.width.toDouble(), image.height.toDouble());
  }

  void _selectRatio(String label, double? ratio, Size imageSize) {
    setState(() {
      _ratioLabel = label;
      _lockedRatio = ratio;
      if (ratio == null) {
        _crop = const Rect.fromLTWH(0.08, 0.08, 0.84, 0.84);
      } else {
        final imageRatio = imageSize.width / imageSize.height;
        double width;
        double height;
        if (imageRatio > ratio) {
          height = 0.84;
          width = height * ratio / imageRatio;
        } else {
          width = 0.84;
          height = width * imageRatio / ratio;
        }
        _crop = Rect.fromCenter(center: const Offset(0.5, 0.5), width: width, height: height);
      }
    });
  }

  _CropHandle _hitTest(Offset p, Size size) {
    const handleRadius = 34.0;
    final left = _crop.left * size.width;
    final right = _crop.right * size.width;
    final top = _crop.top * size.height;
    final bottom = _crop.bottom * size.height;

    bool near(double x, double y) => (p.dx - x).abs() <= handleRadius && (p.dy - y).abs() <= handleRadius;
    if (near(left, top)) return _CropHandle.topLeft;
    if (near(right, top)) return _CropHandle.topRight;
    if (near(left, bottom)) return _CropHandle.bottomLeft;
    if (near(right, bottom)) return _CropHandle.bottomRight;
    if ((p.dx - left).abs() <= handleRadius && p.dy >= top && p.dy <= bottom) return _CropHandle.left;
    if ((p.dx - right).abs() <= handleRadius && p.dy >= top && p.dy <= bottom) return _CropHandle.right;
    if ((p.dy - top).abs() <= handleRadius && p.dx >= left && p.dx <= right) return _CropHandle.top;
    if ((p.dy - bottom).abs() <= handleRadius && p.dx >= left && p.dx <= right) return _CropHandle.bottom;
    if (_crop.contains(Offset(p.dx / size.width, p.dy / size.height))) return _CropHandle.move;
    return _CropHandle.move;
  }

  void _onPanStart(DragStartDetails details, Size size) {
    _previewSize = size;
    _dragStart = details.localPosition;
    _startRect = _crop;
    _handle = _hitTest(details.localPosition, size);
  }

  void _onPanUpdate(DragUpdateDetails details, Size size, Size imageSize) {
    final origin = _dragStart;
    final start = _startRect;
    if (origin == null || start == null) return;

    final dx = details.localPosition.dx / size.width - origin.dx / size.width;
    final dy = details.localPosition.dy / size.height - origin.dy / size.height;
    const minEdge = 0.12;
    var next = start;

    if (_handle == _CropHandle.move) {
      final left = (start.left + dx).clamp(0.0, 1.0 - start.width);
      final top = (start.top + dy).clamp(0.0, 1.0 - start.height);
      next = Rect.fromLTWH(left, top, start.width, start.height);
    } else {
      var left = start.left;
      var top = start.top;
      var right = start.right;
      var bottom = start.bottom;

      if (_handle == _CropHandle.left || _handle == _CropHandle.topLeft || _handle == _CropHandle.bottomLeft) {
        left = (start.left + dx).clamp(0.0, start.right - minEdge);
      }
      if (_handle == _CropHandle.right || _handle == _CropHandle.topRight || _handle == _CropHandle.bottomRight) {
        right = (start.right + dx).clamp(start.left + minEdge, 1.0);
      }
      if (_handle == _CropHandle.top || _handle == _CropHandle.topLeft || _handle == _CropHandle.topRight) {
        top = (start.top + dy).clamp(0.0, start.bottom - minEdge);
      }
      if (_handle == _CropHandle.bottom || _handle == _CropHandle.bottomLeft || _handle == _CropHandle.bottomRight) {
        bottom = (start.bottom + dy).clamp(start.top + minEdge, 1.0);
      }

      if (_lockedRatio != null) {
        final normalizedRatio = _lockedRatio! / (imageSize.width / imageSize.height);
        final width = right - left;
        final height = bottom - top;
        if (width / height > normalizedRatio) {
          final adjustedWidth = height * normalizedRatio;
          if (_handle == _CropHandle.left || _handle == _CropHandle.topLeft || _handle == _CropHandle.bottomLeft) {
            left = right - adjustedWidth;
          } else {
            right = left + adjustedWidth;
          }
        } else {
          final adjustedHeight = width / normalizedRatio;
          if (_handle == _CropHandle.top || _handle == _CropHandle.topLeft || _handle == _CropHandle.topRight) {
            top = bottom - adjustedHeight;
          } else {
            bottom = top + adjustedHeight;
          }
        }
      }
      left = left.clamp(0.0, 1.0);
      top = top.clamp(0.0, 1.0);
      right = right.clamp(left + minEdge, 1.0);
      bottom = bottom.clamp(top + minEdge, 1.0);
      next = Rect.fromLTRB(left, top, right, bottom);
    }

    setState(() => _crop = next);
  }

  Future<void> _export() async {
    setState(() => _processing = true);
    try {
      final result = await ImageEngine.cropToPng(
        widget.originalBytes,
        left: _crop.left,
        top: _crop.top,
        width: _crop.width,
        height: _crop.height,
      );
      await ImageExporter.saveToGallery(
        bytes: result,
        fileName: 'ImageTools_Crop_${DateTime.now().millisecondsSinceEpoch}.png',
        mimeType: 'image/png',
      );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Imagem recortada e guardada em Pictures/Image Tools.')),
      );
      Navigator.of(context).pop(true);
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(error.toString())));
    } finally {
      if (mounted) setState(() => _processing = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(
        title: const Text('Editar e recortar'),
        actions: [
          TextButton(
            onPressed: _processing ? null : () => setState(() => _crop = const Rect.fromLTWH(0.08, 0.08, 0.84, 0.84)),
            child: const Text('Repor'),
          ),
        ],
      ),
      body: FutureBuilder<Size>(
        future: _imageSizeFuture,
        builder: (context, snapshot) {
          if (snapshot.connectionState != ConnectionState.done) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snapshot.hasError || !snapshot.hasData) {
            return const Center(child: Text('Não foi possível abrir esta imagem.'));
          }
          final imageSize = snapshot.data!;
          return LayoutBuilder(
            builder: (context, constraints) {
              final maxWidth = constraints.maxWidth - 32;
              final aspect = imageSize.width / imageSize.height;
              var displayWidth = maxWidth;
              var displayHeight = displayWidth / aspect;
              if (displayHeight > 430) {
                displayHeight = 430;
                displayWidth = displayHeight * aspect;
              }
              final preview = Size(displayWidth, displayHeight);
              return Column(
                children: [
                  Padding(
                    padding: const EdgeInsets.fromLTRB(16, 10, 16, 8),
                    child: Row(
                      children: [
                        const Icon(Icons.touch_app_outlined, color: AppTheme.cyan),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            'Arraste a moldura ou use os cantos e as laterais para ajustar o corte.',
                            style: Theme.of(context).textTheme.bodySmall,
                          ),
                        ),
                      ],
                    ),
                  ),
                  Expanded(
                    child: Center(
                      child: SizedBox(
                        width: displayWidth,
                        height: displayHeight,
                        child: GestureDetector(
                          behavior: HitTestBehavior.opaque,
                          onPanStart: (details) => _onPanStart(details, preview),
                          onPanUpdate: (details) => _onPanUpdate(details, preview, imageSize),
                          child: Stack(
                            fit: StackFit.expand,
                            children: [
                              Image.memory(widget.originalBytes, fit: BoxFit.fill, gaplessPlayback: true),
                              CustomPaint(painter: _CropOverlayPainter(crop: _crop)),
                            ],
                          ),
                        ),
                      ),
                    ),
                  ),
                  SizedBox(
                    height: 68,
                    child: ListView(
                      scrollDirection: Axis.horizontal,
                      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                      children: [
                        _ratioChip('Livre', null, imageSize),
                        _ratioChip('1:1', 1, imageSize),
                        _ratioChip('4:5', 4 / 5, imageSize),
                        _ratioChip('16:9', 16 / 9, imageSize),
                        _ratioChip('9:16', 9 / 16, imageSize),
                      ],
                    ),
                  ),
                  Padding(
                    padding: const EdgeInsets.fromLTRB(16, 8, 16, 20),
                    child: FilledButton.icon(
                      onPressed: _processing ? null : _export,
                      icon: _processing
                          ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                          : const Icon(Icons.crop),
                      label: Text(_processing ? 'A processar imagem…' : 'Aplicar recorte e guardar'),
                    ),
                  ),
                ],
              );
            },
          );
        },
      ),
    );
  }

  Widget _ratioChip(String label, double? ratio, Size imageSize) {
    final selected = _ratioLabel == label;
    return Padding(
      padding: const EdgeInsets.only(right: 8),
      child: ChoiceChip(
        label: Text(label),
        selected: selected,
        onSelected: (_) => _selectRatio(label, ratio, imageSize),
      ),
    );
  }
}

class _CropOverlayPainter extends CustomPainter {
  const _CropOverlayPainter({required this.crop});

  final Rect crop;

  @override
  void paint(Canvas canvas, Size size) {
    final rect = Rect.fromLTRB(
      crop.left * size.width,
      crop.top * size.height,
      crop.right * size.width,
      crop.bottom * size.height,
    );
    final shade = Paint()..color = Colors.black.withValues(alpha: 0.56);
    canvas.drawRect(Rect.fromLTRB(0, 0, size.width, rect.top), shade);
    canvas.drawRect(Rect.fromLTRB(0, rect.bottom, size.width, size.height), shade);
    canvas.drawRect(Rect.fromLTRB(0, rect.top, rect.left, rect.bottom), shade);
    canvas.drawRect(Rect.fromLTRB(rect.right, rect.top, size.width, rect.bottom), shade);

    final grid = Paint()
      ..color = Colors.white.withValues(alpha: 0.68)
      ..strokeWidth = 0.8
      ..style = PaintingStyle.stroke;
    for (var i = 1; i < 3; i++) {
      final x = rect.left + rect.width * i / 3;
      final y = rect.top + rect.height * i / 3;
      canvas.drawLine(Offset(x, rect.top), Offset(x, rect.bottom), grid);
      canvas.drawLine(Offset(rect.left, y), Offset(rect.right, y), grid);
    }

    final border = Paint()
      ..color = Colors.white
      ..strokeWidth = 2.4
      ..style = PaintingStyle.stroke;
    canvas.drawRect(rect, border);

    final handle = Paint()
      ..color = AppTheme.cyan
      ..strokeWidth = 5
      ..strokeCap = StrokeCap.round;
    const length = 19.0;
    final corners = <List<Offset>>[
      [rect.topLeft, Offset(rect.left + length, rect.topLeft.dy)],
      [rect.topLeft, Offset(rect.left, rect.top + length)],
      [rect.topRight, Offset(rect.right - length, rect.top)],
      [rect.topRight, Offset(rect.right, rect.top + length)],
      [rect.bottomLeft, Offset(rect.left + length, rect.bottom)],
      [rect.bottomLeft, Offset(rect.left, rect.bottom - length)],
      [rect.bottomRight, Offset(rect.right - length, rect.bottom)],
      [rect.bottomRight, Offset(rect.right, rect.bottom - length)],
    ];
    for (final segment in corners) {
      canvas.drawLine(segment[0], segment[1], handle);
    }
  }

  @override
  bool shouldRepaint(covariant _CropOverlayPainter oldDelegate) => oldDelegate.crop != crop;
}
