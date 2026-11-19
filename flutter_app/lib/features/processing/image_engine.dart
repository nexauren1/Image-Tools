import 'dart:isolate';
import 'dart:typed_data';

import 'package:image/image.dart' as img;

enum ImageOutputFormat { jpeg, png, webp }

class ImageDetails {
  const ImageDetails({
    required this.width,
    required this.height,
    required this.format,
    required this.bytes,
    required this.hasAlpha,
  });

  final int width;
  final int height;
  final String format;
  final int bytes;
  final bool hasAlpha;
}

abstract final class ImageEngine {
  static const int maxPixels = 40 * 1000 * 1000;

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

  static Future<ImageDetails> inspect(Uint8List bytes) {
    return Isolate.run(() {
      final decoded = img.decodeImage(bytes);
      if (decoded == null) {
        throw const FormatException('O ficheiro não contém uma imagem válida.');
      }
      final oriented = img.bakeOrientation(decoded);
      if (oriented.width * oriented.height > maxPixels) {
        throw const FormatException('A imagem excede o limite seguro de processamento.');
      }
      return ImageDetails(
        width: oriented.width,
        height: oriented.height,
        format: decoded.format.name.toUpperCase(),
        bytes: bytes.length,
        hasAlpha: oriented.hasAlpha,
      );
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

      if (left < 0 || top < 0 || width <= 0 || height <= 0 ||
          left + width > 1.000001 || top + height > 1.000001) {
        throw const FormatException('A área de recorte é inválida.');
      }

      final x = (left * source.width).round();
      final y = (top * source.height).round();
      final cropWidth = (width * source.width).round();
      final cropHeight = (height * source.height).round();
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
      final sourceFormat = decoded.format.name.toLowerCase();
      final sameFormat = switch (format) {
        ImageOutputFormat.jpeg => sourceFormat == 'jpeg' || sourceFormat == 'jpg',
        ImageOutputFormat.png => sourceFormat == 'png',
        ImageOutputFormat.webp => sourceFormat == 'webp',
      };
      // Selecting the current format is a no-op: preserve the exact encoded
      // bytes, metadata, original resolution and any camera colour profile.
      if (sameFormat) return Uint8List.fromList(originalBytes);

      final source = img.bakeOrientation(decoded);
      if (source.width * source.height > maxPixels) {
        throw const FormatException('A imagem excede o limite seguro de processamento.');
      }
      return _encode(source, format, quality);
    });
  }

  static Future<Uint8List> compress(
    Uint8List originalBytes, {
    required ImageOutputFormat format,
    required int quality,
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
      if (format == ImageOutputFormat.png) {
        throw const FormatException('PNG não utiliza um nível de qualidade JPEG. Use JPEG ou WebP para compressão com perdas.');
      }
      return _encode(source, format, quality);
    });
  }

  static Future<Uint8List> rotate(
    Uint8List originalBytes, {
    required int quarterTurns,
    bool flipHorizontal = false,
    bool flipVertical = false,
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
      var result = source;
      final turns = ((quarterTurns % 4) + 4) % 4;
      if (turns != 0) result = img.copyRotate(result, angle: turns * 90);
      if (flipHorizontal) result = img.flipHorizontal(result);
      if (flipVertical) result = img.flipVertical(result);
      return Uint8List.fromList(img.encodePng(result, level: 6));
    });
  }

  static Future<Uint8List> applyFilter(
    Uint8List originalBytes, {
    required String filter,
    double strength = 1,
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

      final amount = strength.clamp(0.0, 1.0);
      final img.Image result;
      switch (filter) {
        case 'grayscale':
          result = img.grayscale(source);
          break;
        case 'sepia':
          result = img.sepia(source);
          break;
        case 'negative':
          result = img.invert(source);
          break;
        case 'bright':
          result = img.adjustColor(source, brightness: 1 + amount);
          break;
        case 'contrast':
          result = img.adjustColor(source, contrast: 1 + amount);
          break;
        case 'saturate':
          result = img.adjustColor(source, saturation: 1 + amount);
          break;
        case 'blur':
          result = img.gaussianBlur(source, radius: amount.round().clamp(1, 12));
          break;
        case 'sharpen':
          result = img.convolution(source, filter: [0, -1, 0, -1, 5, -1, 0, -1, 0]);
          break;
        default:
          throw ArgumentError.value(filter, 'filter', 'Filtro não suportado.');
      }
      return Uint8List.fromList(img.encodePng(result, level: 6));
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
