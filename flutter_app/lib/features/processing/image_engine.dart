import 'dart:isolate';
import 'dart:typed_data';

import 'package:image/image.dart' as img;

enum ImageOutputFormat { jpeg, png, webp }

abstract final class ImageEngine {
  static const int maxPixels = 80 * 1000 * 1000;

  static Future<img.Image> decodeForPreview(Uint8List bytes) {
    return Isolate.run(() {
      final decoded = img.decodeImage(bytes);
      if (decoded == null) {
        throw const FormatException('O ficheiro não contém uma imagem válida.');
      }
      final oriented = img.bakeOrientation(decoded);
      if (oriented.width * oriented.height > maxPixels) {
        throw const FormatException('A imagem excede o limite seguro de processamento deste dispositivo.');
      }
      return oriented;
    });
  }

  static Future<Uint8List> cropToPng(
    Uint8List originalBytes, {
    required double left,
    required double top,
    required double width,
    required double height,
  }) {
    return Isolate.run(() {
      final decoded = img.decodeImage(originalBytes);
      if (decoded == null) {
        throw const FormatException('Não foi possível descodificar a imagem.');
      }
      final source = img.bakeOrientation(decoded);
      if (source.width * source.height > maxPixels) {
        throw const FormatException('A imagem excede o limite seguro de processamento.');
      }

      final x = (left.clamp(0.0, 1.0) * source.width).round();
      final y = (top.clamp(0.0, 1.0) * source.height).round();
      final cropWidth = (width.clamp(0.0, 1.0) * source.width).round();
      final cropHeight = (height.clamp(0.0, 1.0) * source.height).round();
      if (cropWidth < 1 || cropHeight < 1 || x + cropWidth > source.width || y + cropHeight > source.height) {
        throw const FormatException('A área de recorte é inválida.');
      }

      final cropped = img.copyCrop(
        source,
        x: x,
        y: y,
        width: cropWidth,
        height: cropHeight,
      );
      return Uint8List.fromList(img.encodePng(cropped, level: 6));
    });
  }

  static Future<Uint8List> resize(
    Uint8List originalBytes, {
    required int width,
    required int height,
    ImageOutputFormat format = ImageOutputFormat.png,
    int quality = 92,
  }) {
    return Isolate.run(() {
      final decoded = img.decodeImage(originalBytes);
      if (decoded == null) {
        throw const FormatException('Não foi possível descodificar a imagem.');
      }
      final source = img.bakeOrientation(decoded);
      if (width < 1 || height < 1 || width > 12000 || height > 12000 || width * height > maxPixels) {
        throw const FormatException('As dimensões escolhidas excedem o limite seguro.');
      }
      final resized = img.copyResize(
        source,
        width: width,
        height: height,
        interpolation: img.Interpolation.cubic,
      );
      return _encode(resized, format, quality);
    });
  }

  static Future<Uint8List> convert(
    Uint8List originalBytes, {
    required ImageOutputFormat format,
    int quality = 92,
  }) {
    return Isolate.run(() {
      final decoded = img.decodeImage(originalBytes);
      if (decoded == null) {
        throw const FormatException('Não foi possível descodificar a imagem.');
      }
      final source = img.bakeOrientation(decoded);
      if (source.width * source.height > maxPixels) {
        throw const FormatException('A imagem excede o limite seguro de processamento.');
      }
      return _encode(source, format, quality);
    });
  }

  static Future<Uint8List> enhance(
    Uint8List originalBytes, {
    required double brightness,
    required double contrast,
    required double saturation,
  }) {
    return Isolate.run(() {
      final decoded = img.decodeImage(originalBytes);
      if (decoded == null) {
        throw const FormatException('Não foi possível descodificar a imagem.');
      }
      final source = img.bakeOrientation(decoded);
      if (source.width * source.height > maxPixels) {
        throw const FormatException('A imagem excede o limite seguro de processamento.');
      }
      final result = img.adjustColor(
        source,
        brightness: brightness,
        contrast: contrast,
        saturation: saturation,
      );
      return Uint8List.fromList(img.encodePng(result, level: 6));
    });
  }

  static Uint8List _encode(img.Image image, ImageOutputFormat format, int quality) {
    final safeQuality = quality.clamp(1, 100);
    switch (format) {
      case ImageOutputFormat.jpeg:
        return Uint8List.fromList(img.encodeJpg(image, quality: safeQuality));
      case ImageOutputFormat.png:
        return Uint8List.fromList(img.encodePng(image, level: 6));
      case ImageOutputFormat.webp:
        return Uint8List.fromList(img.encodeWebP(image, quality: safeQuality));
    }
  }
}
