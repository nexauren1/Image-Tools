import 'dart:isolate';
import 'dart:math' as math;
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
    int maxDimension = 0,
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
      final limit = maxDimension.clamp(0, 12000);
      final processed = limit > 0 && math.max(source.width, source.height) > limit
          ? img.copyResize(
              source,
              width: math.max(1, (source.width * limit / math.max(source.width, source.height)).round()),
              height: math.max(1, (source.height * limit / math.max(source.width, source.height)).round()),
              interpolation: img.Interpolation.cubic,
            )
          : source;
      return _encode(processed, format, quality);
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

  static Future<Uint8List> sketch(
    Uint8List originalBytes, {
    double strength = 0.7,
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

      final gray = img.grayscale(source);
      final inverted = img.invert(gray.clone());
      final amount = strength.clamp(0.0, 1.0);
      final radius = (2 + amount * 7).round().clamp(2, 9).toInt();
      final softened = img.gaussianBlur(inverted, radius: radius);
      final output = img.Image(
        width: source.width,
        height: source.height,
        numChannels: 4,
      );

      for (var y = 0; y < source.height; y++) {
        for (var x = 0; x < source.width; x++) {
          final basePixel = gray.getPixel(x, y);
          final blurPixel = softened.getPixel(x, y);
          final base = basePixel.r.toDouble();
          final mask = blurPixel.r.toDouble();
          final divisor = math.max(1.0, 255.0 - mask);
          final pencil = (base * 255.0 / divisor).clamp(0.0, 255.0);
          final value = (base * (1.0 - amount) + pencil * amount)
              .round()
              .clamp(0, 255);
          output.setPixelRgba(
            x,
            y,
            value,
            value,
            value,
            basePixel.a.toInt(),
          );
        }
      }
      return Uint8List.fromList(img.encodePng(output, level: 6));
    });
  }

  static Future<Uint8List> vignette(
    Uint8List originalBytes, {
    double strength = 0.65,
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

      final output = source.clone();
      final amount = strength.clamp(0.0, 1.0);
      final centerX = (source.width - 1) / 2.0;
      final centerY = (source.height - 1) / 2.0;
      final scaleX = math.max(1.0, source.width / 2.0);
      final scaleY = math.max(1.0, source.height / 2.0);
      final cornerDistance = math.sqrt(2.0);

      for (var y = 0; y < source.height; y++) {
        for (var x = 0; x < source.width; x++) {
          final dx = (x - centerX) / scaleX;
          final dy = (y - centerY) / scaleY;
          final distance = (math.sqrt(dx * dx + dy * dy) / cornerDistance)
              .clamp(0.0, 1.0);
          final edge = ((distance - 0.28) / 0.72).clamp(0.0, 1.0);
          final smooth = edge * edge * (3.0 - 2.0 * edge);
          final factor = 1.0 - amount * smooth * 0.78;
          final pixel = source.getPixel(x, y);
          output.setPixelRgba(
            x,
            y,
            (pixel.r * factor).round().clamp(0, 255),
            (pixel.g * factor).round().clamp(0, 255),
            (pixel.b * factor).round().clamp(0, 255),
            pixel.a.toInt(),
          );
        }
      }
      return Uint8List.fromList(img.encodePng(output, level: 6));
    });
  }

  static Future<Uint8List> outline(
    Uint8List originalBytes, {
    double strength = 0.7,
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

      final gray = img.grayscale(source);
      final output = img.Image(
        width: source.width,
        height: source.height,
        numChannels: 4,
      );
      final threshold = (245.0 - strength.clamp(0.0, 1.0) * 190.0)
          .clamp(35.0, 245.0);

      double luminance(int x, int y) {
        final safeX = x.clamp(0, source.width - 1).toInt();
        final safeY = y.clamp(0, source.height - 1).toInt();
        return gray.getPixel(safeX, safeY).r.toDouble();
      }

      for (var y = 0; y < source.height; y++) {
        for (var x = 0; x < source.width; x++) {
          final gx = -luminance(x - 1, y - 1) + luminance(x + 1, y - 1)
              - 2.0 * luminance(x - 1, y) + 2.0 * luminance(x + 1, y)
              - luminance(x - 1, y + 1) + luminance(x + 1, y + 1);
          final gy = -luminance(x - 1, y - 1) - 2.0 * luminance(x, y - 1)
              - luminance(x + 1, y - 1) + luminance(x - 1, y + 1)
              + 2.0 * luminance(x, y + 1) + luminance(x + 1, y + 1);
          final magnitude = math.sqrt(gx * gx + gy * gy);
          final shade = magnitude >= threshold ? 18 : 255;
          final alpha = source.getPixel(x, y).a.toInt();
          output.setPixelRgba(x, y, shade, shade, shade, alpha);
        }
      }
      return Uint8List.fromList(img.encodePng(output, level: 6));
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
        case 'warm':
          result = img.adjustColor(source, saturation: 1 + amount * 0.12, brightness: 1 + amount * 0.025);
          for (var y = 0; y < result.height; y++) {
            for (var x = 0; x < result.width; x++) {
              final p = result.getPixel(x, y);
              result.setPixelRgba(x, y, (p.r + amount * 22).round().clamp(0, 255), p.g, (p.b - amount * 16).round().clamp(0, 255), p.a);
            }
          }
          break;
        case 'cool':
          result = img.adjustColor(source, saturation: 1 + amount * 0.08, brightness: 1 + amount * 0.02);
          for (var y = 0; y < result.height; y++) {
            for (var x = 0; x < result.width; x++) {
              final p = result.getPixel(x, y);
              result.setPixelRgba(x, y, (p.r - amount * 14).round().clamp(0, 255), p.g, (p.b + amount * 24).round().clamp(0, 255), p.a);
            }
          }
          break;
        case 'vintage':
          result = img.adjustColor(img.sepia(source), contrast: 0.92, saturation: 0.78, brightness: 1.03);
          break;
        case 'fade':
          result = img.adjustColor(source, contrast: 1 - amount * 0.22, saturation: 1 - amount * 0.18, brightness: 1 + amount * 0.06);
          break;
        case 'noir':
          result = img.adjustColor(
            img.grayscale(source),
            contrast: 1.08 + amount * 0.45,
            brightness: 0.98,
          );
          break;
        case 'pastel':
          result = img.adjustColor(
            source,
            saturation: 1 - amount * 0.48,
            brightness: 1 + amount * 0.10,
            contrast: 1 - amount * 0.10,
          );
          break;
        case 'cinematic':
          final cinematic = img.adjustColor(
            source,
            contrast: 1 + amount * 0.20,
            saturation: 1 - amount * 0.06,
          );
          for (var y = 0; y < cinematic.height; y++) {
            for (var x = 0; x < cinematic.width; x++) {
              final pixel = cinematic.getPixel(x, y);
              final luma = 0.2126 * pixel.r + 0.7152 * pixel.g + 0.0722 * pixel.b;
              final highlight = ((luma - 100.0) / 155.0).clamp(0.0, 1.0);
              final shadow = 1.0 - highlight;
              cinematic.setPixelRgba(
                x,
                y,
                (pixel.r + amount * (18.0 * highlight - 5.0 * shadow)).round().clamp(0, 255),
                (pixel.g + amount * (5.0 * highlight + 7.0 * shadow)).round().clamp(0, 255),
                (pixel.b + amount * (-10.0 * highlight + 18.0 * shadow)).round().clamp(0, 255),
                pixel.a.toInt(),
              );
            }
          }
          result = cinematic;
          break;
        case 'solarize':
          final solarized = source.clone();
          final threshold = 255.0 - amount * 150.0;
          for (var y = 0; y < solarized.height; y++) {
            for (var x = 0; x < solarized.width; x++) {
              final pixel = source.getPixel(x, y);
              final luma = 0.2126 * pixel.r + 0.7152 * pixel.g + 0.0722 * pixel.b;
              if (luma > threshold) {
                solarized.setPixelRgba(
                  x,
                  y,
                  (255 - pixel.r).round().clamp(0, 255),
                  (255 - pixel.g).round().clamp(0, 255),
                  (255 - pixel.b).round().clamp(0, 255),
                  pixel.a.toInt(),
                );
              }
            }
          }
          result = solarized;
          break;
        case 'posterize':
          final levels = (8 - amount * 5).round().clamp(2, 8).toInt();
          final step = 255.0 / (levels - 1);
          final posterized = source.clone();
          for (var y = 0; y < posterized.height; y++) {
            for (var x = 0; x < posterized.width; x++) {
              final pixel = source.getPixel(x, y);
              int quantize(num channel) =>
                  ((channel / step).round() * step).round().clamp(0, 255).toInt();
              posterized.setPixelRgba(
                x,
                y,
                quantize(pixel.r),
                quantize(pixel.g),
                quantize(pixel.b),
                pixel.a.toInt(),
              );
            }
          }
          result = posterized;
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
    double temperature = 0,
    double tint = 0,
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
      final warm = temperature.clamp(-100, 100) / 100;
      final greenShift = tint.clamp(-100, 100) / 100;
      if (warm != 0 || greenShift != 0) {
        for (var y = 0; y < result.height; y++) {
          for (var x = 0; x < result.width; x++) {
            final pixel = result.getPixel(x, y);
            result.setPixelRgba(
              x,
              y,
              (pixel.r + warm * 24).round().clamp(0, 255),
              (pixel.g + greenShift * 16).round().clamp(0, 255),
              (pixel.b - warm * 24).round().clamp(0, 255),
              pixel.a,
            );
          }
        }
      }
      return Uint8List.fromList(img.encodePng(result, level: 6));
    });
  }



  static Future<Uint8List> autoEnhance(Uint8List originalBytes) {
    return Isolate.run(() {
      final decoded = img.decodeImage(originalBytes);
      if (decoded == null) {
        throw const FormatException('Não foi possível descodificar a imagem.');
      }
      final source = img.bakeOrientation(decoded);
      if (source.width * source.height > maxPixels) {
        throw const FormatException('A imagem excede o limite seguro de processamento.');
      }

      final strideX = (source.width / 160).ceil().clamp(1, source.width).toInt();
      final strideY = (source.height / 160).ceil().clamp(1, source.height).toInt();
      var luminanceSum = 0.0;
      var samples = 0;
      for (var y = 0; y < source.height; y += strideY) {
        for (var x = 0; x < source.width; x += strideX) {
          final pixel = source.getPixel(x, y);
          luminanceSum += 0.2126 * pixel.r + 0.7152 * pixel.g + 0.0722 * pixel.b;
          samples++;
        }
      }
      final average = samples == 0 ? 128.0 : luminanceSum / samples;
      final brightness = (1 + (128 - average) / 520).clamp(0.82, 1.22).toDouble();
      final result = img.adjustColor(
        source,
        brightness: brightness,
        contrast: 1.12,
        saturation: 1.08,
      );
      return Uint8List.fromList(img.encodePng(result, level: 6));
    });
  }

  static Future<Uint8List> addTextWatermark(
    Uint8List originalBytes, {
    required String text,
    double opacity = 0.72,
    String position = 'bottomRight',
    int fontSize = 24,
    int red = 255,
    int green = 255,
    int blue = 255,
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
      final cleanText = text.trim();
      if (cleanText.isEmpty) {
        throw const FormatException('Escreve o texto da marca de água.');
      }
      final safeOpacity = (opacity.clamp(0.12, 1.0) * 255).round();
      final useLargeFont = fontSize >= 36;
      final font = useLargeFont ? img.arial48 : img.arial24;
      final estimatedWidth = (cleanText.length * (useLargeFont ? 28 : 14))
          .clamp(1, source.width - 16).toInt();
      final margin = (source.width * 0.025).round().clamp(8, 64).toInt();
      final int x = switch (position) {
        'topLeft' || 'bottomLeft' => margin,
        'topRight' || 'bottomRight' =>
          (source.width - estimatedWidth - margin).clamp(0, source.width - 1).toInt(),
        _ => ((source.width - estimatedWidth) / 2).round().clamp(0, source.width - 1).toInt(),
      };
      final int y = switch (position) {
        'topLeft' || 'topRight' => margin,
        'center' => ((source.height - font.lineHeight) / 2).round(),
        _ => (source.height - font.lineHeight - margin).clamp(0, source.height - 1).toInt(),
      };
      img.drawString(
        source,
        cleanText,
        font: font,
        x: x,
        y: y,
        color: img.ColorRgba8(
          red.clamp(0, 255).toInt(),
          green.clamp(0, 255).toInt(),
          blue.clamp(0, 255).toInt(),
          safeOpacity,
        ),
        wrap: true,
      );
      return Uint8List.fromList(img.encodePng(source, level: 6));
    });
  }

  static Future<Uint8List> addBorder(
    Uint8List originalBytes, {
    int thickness = 28,
    int red = 255,
    int green = 255,
    int blue = 255,
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
      final pad = thickness.clamp(2, 240).toInt();
      final width = source.width + pad * 2;
      final height = source.height + pad * 2;
      if (width * height > maxPixels) {
        throw const FormatException('A moldura escolhida excede o limite de memória seguro.');
      }
      final result = img.copyExpandCanvas(
        source,
        padding: pad,
        backgroundColor: img.ColorRgba8(
          red.clamp(0, 255).toInt(),
          green.clamp(0, 255).toInt(),
          blue.clamp(0, 255).toInt(),
          255,
        ),
      );
      return Uint8List.fromList(img.encodePng(result, level: 6));
    });
  }

  static Future<Uint8List> pixelate(
    Uint8List originalBytes, {
    int blockSize = 18,
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
      final block = blockSize.clamp(3, 96).toInt();
      final smallWidth = (source.width / block).ceil().clamp(1, source.width).toInt();
      final smallHeight = (source.height / block).ceil().clamp(1, source.height).toInt();
      final reduced = img.copyResize(
        source,
        width: smallWidth,
        height: smallHeight,
        interpolation: img.Interpolation.average,
      );
      final result = img.copyResize(
        reduced,
        width: source.width,
        height: source.height,
        interpolation: img.Interpolation.nearest,
      );
      return Uint8List.fromList(img.encodePng(result, level: 6));
    });
  }

  static Future<Uint8List> fitSocialPreset(
    Uint8List originalBytes, {
    required int width,
    required int height,
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
      if (width < 1 || height < 1 || width > 3000 || height > 3000 ||
          width * height > 6 * 1000 * 1000) {
        throw const FormatException('As dimensões sociais escolhidas não são válidas.');
      }
      final scale = math.max(width / source.width, height / source.height);
      final scaledWidth = (source.width * scale).ceil();
      final scaledHeight = (source.height * scale).ceil();
      final resized = img.copyResize(
        source,
        width: scaledWidth,
        height: scaledHeight,
        interpolation: img.Interpolation.cubic,
      );
      final left = ((scaledWidth - width) / 2).floor();
      final top = ((scaledHeight - height) / 2).floor();
      final result = img.copyCrop(
        resized,
        x: left,
        y: top,
        width: width,
        height: height,
      );
      return Uint8List.fromList(img.encodePng(result, level: 6));
    });
  }

  static Future<List<String>> extractPalette(
    Uint8List originalBytes, {
    int colorCount = 6,
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
      final stepX = (source.width / 120).ceil().clamp(1, source.width).toInt();
      final stepY = (source.height / 120).ceil().clamp(1, source.height).toInt();
      final histogram = <int, int>{};
      for (var y = 0; y < source.height; y += stepY) {
        for (var x = 0; x < source.width; x += stepX) {
          final pixel = source.getPixel(x, y);
          final r = pixel.r.toInt() & 0xF0;
          final g = pixel.g.toInt() & 0xF0;
          final b = pixel.b.toInt() & 0xF0;
          final key = (r << 16) | (g << 8) | b;
          histogram.update(key, (value) => value + 1, ifAbsent: () => 1);
        }
      }
      final sorted = histogram.entries.toList()
        ..sort((a, b) => b.value.compareTo(a.value));
      final output = <String>[];
      for (final entry in sorted) {
        final hex = entry.key.toRadixString(16).padLeft(6, '0').toUpperCase();
        if (output.isEmpty || output.last != '#$hex') output.add('#$hex');
        if (output.length >= colorCount.clamp(1, 10).toInt()) break;
      }
      return output;
    });
  }

  static Future<Uint8List> createCollage(
    List<Uint8List> images, {
    int canvasSize = 1200,
    int gap = 18,
    int columns = 0,
    String background = 'navy',
    String fitMode = 'contain',
  }) {
    return Isolate.run(() {
      if (images.length < 2 || images.length > 9) {
        throw const FormatException('Escolhe entre 2 e 9 imagens para a colagem.');
      }
      final safeColumns = (columns <= 0 ? math.sqrt(images.length).ceil() : columns)
          .clamp(1, 3)
          .toInt();
      final rows = (images.length / safeColumns).ceil();
      final size = canvasSize.clamp(600, 2400).toInt();
      final spacing = gap.clamp(0, 48).toInt();
      final cellWidth = (size - spacing * (safeColumns + 1)) ~/ safeColumns;
      final cellHeight = (size - spacing * (rows + 1)) ~/ rows;
      // Keep the selected square canvas size exact. Integer tile rounding may
      // leave a few background pixels at the trailing edge.
      final canvasWidth = size;
      final canvasHeight = size;
      if (canvasWidth * canvasHeight > maxPixels) {
        throw const FormatException('A colagem excede o limite de memória seguro.');
      }
      final backgroundColor = switch (background) {
        'white' => img.ColorRgba8(255, 255, 255, 255),
        'black' => img.ColorRgba8(0, 0, 0, 255),
        'transparent' => img.ColorRgba8(0, 0, 0, 0),
        _ => img.ColorRgba8(12, 18, 33, 255),
      };
      final canvas = img.Image(
        width: canvasWidth,
        height: canvasHeight,
        numChannels: 4,
        backgroundColor: backgroundColor,
      );
      for (var index = 0; index < images.length; index++) {
        final decoded = img.decodeImage(images[index]);
        if (decoded == null) continue;
        final source = img.bakeOrientation(decoded);
        if (source.width * source.height > maxPixels) {
          throw const FormatException('Uma das imagens excede o limite seguro.');
        }
        img.Image tile;
        if (fitMode == 'cover') {
          final scale = math.max(cellWidth / source.width, cellHeight / source.height);
          final scaled = img.copyResize(
            source,
            width: (source.width * scale).ceil(),
            height: (source.height * scale).ceil(),
            interpolation: img.Interpolation.cubic,
          );
          final cropX = ((scaled.width - cellWidth) / 2).floor();
          final cropY = ((scaled.height - cellHeight) / 2).floor();
          tile = img.copyCrop(
            scaled,
            x: cropX,
            y: cropY,
            width: cellWidth,
            height: cellHeight,
          );
        } else {
          tile = img.copyResize(
            source,
            width: cellWidth,
            height: cellHeight,
            maintainAspect: true,
            backgroundColor: backgroundColor,
            interpolation: img.Interpolation.cubic,
          );
        }
        final column = index % safeColumns;
        final row = index ~/ safeColumns;
        img.compositeImage(
          canvas,
          tile,
          dstX: spacing + column * (cellWidth + spacing),
          dstY: spacing + row * (cellHeight + spacing),
        );
      }
      return Uint8List.fromList(img.encodePng(canvas, level: 6));
    });
  }

  static Future<List<Uint8List>> splitIntoGrid(
    Uint8List originalBytes, {
    int rows = 3,
    int columns = 3,
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
      final safeRows = rows.clamp(2, 4).toInt();
      final safeColumns = columns.clamp(2, 4).toInt();
      final outputs = <Uint8List>[];
      for (var row = 0; row < safeRows; row++) {
        for (var column = 0; column < safeColumns; column++) {
          final x = (column * source.width) ~/ safeColumns;
          final y = (row * source.height) ~/ safeRows;
          final right = ((column + 1) * source.width) ~/ safeColumns;
          final bottom = ((row + 1) * source.height) ~/ safeRows;
          final piece = img.copyCrop(
            source,
            x: x,
            y: y,
            width: right - x,
            height: bottom - y,
          );
          outputs.add(Uint8List.fromList(img.encodePng(piece, level: 6)));
        }
      }
      return outputs;
    });
  }

  static Future<List<Uint8List>> batchCompress(
    List<Uint8List> images, {
    int quality = 82,
    ImageOutputFormat format = ImageOutputFormat.jpeg,
    int maxDimension = 0,
  }) {
    return Isolate.run(() {
      if (images.isEmpty || images.length > 30) {
        throw const FormatException('Escolhe entre 1 e 30 imagens para um processamento estável.');
      }
      if (format == ImageOutputFormat.png) {
        throw const FormatException('Para compressão em lote, escolhe JPEG ou WebP.');
      }
      final output = <Uint8List>[];
      for (final bytes in images) {
        final decoded = img.decodeImage(bytes);
        if (decoded == null) continue;
        final source = img.bakeOrientation(decoded);
        if (source.width * source.height > maxPixels) {
          throw const FormatException('Uma imagem excede o limite seguro de processamento.');
        }
        final limit = maxDimension.clamp(0, 12000);
        final processed = limit > 0 && math.max(source.width, source.height) > limit
            ? img.copyResize(
                source,
                width: math.max(1, (source.width * limit / math.max(source.width, source.height)).round()),
                height: math.max(1, (source.height * limit / math.max(source.width, source.height)).round()),
                interpolation: img.Interpolation.cubic,
              )
            : source;
        output.add(_encode(processed, format, quality));
      }
      if (output.isEmpty) {
        throw const FormatException('Não foi possível processar nenhuma imagem.');
      }
      return output;
    });
  }


  static Future<Uint8List> stripMetadataToPng(Uint8List originalBytes) {
    return Isolate.run(() {
      final decoded = img.decodeImage(originalBytes);
      if (decoded == null) {
        throw const FormatException('Não foi possível descodificar a imagem.');
      }
      final source = img.bakeOrientation(decoded);
      if (source.width * source.height > maxPixels) {
        throw const FormatException('A imagem excede o limite seguro de processamento.');
      }
      // Always re-encode. Same-format conversion is deliberately a byte-preserving no-op.
      return Uint8List.fromList(img.encodePng(source, level: 6));
    });
  }

  static Future<Uint8List> replaceColor(
    Uint8List originalBytes, {
    required int targetRed,
    required int targetGreen,
    required int targetBlue,
    required int replacementRed,
    required int replacementGreen,
    required int replacementBlue,
    int tolerance = 72,
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
      final output = source.clone();
      final limit = tolerance.clamp(1, 441);
      final squaredLimit = limit * limit;
      for (var y = 0; y < source.height; y++) {
        for (var x = 0; x < source.width; x++) {
          final pixel = source.getPixel(x, y);
          final dr = pixel.r.toInt() - targetRed;
          final dg = pixel.g.toInt() - targetGreen;
          final db = pixel.b.toInt() - targetBlue;
          final distanceSquared = dr * dr + dg * dg + db * db;
          if (distanceSquared <= squaredLimit) {
            output.setPixelRgba(
              x,
              y,
              replacementRed.clamp(0, 255),
              replacementGreen.clamp(0, 255),
              replacementBlue.clamp(0, 255),
              pixel.a,
            );
          }
        }
      }
      return Uint8List.fromList(img.encodePng(output, level: 6));
    });
  }

  static Future<Uint8List> roundCorners(
    Uint8List originalBytes, {
    int radius = 48,
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
      final safeRadius = radius.clamp(1, math.min(source.width, source.height) ~/ 2).toInt();
      final output = img.Image(
        width: source.width,
        height: source.height,
        numChannels: 4,
      );
      img.compositeImage(output, source);
      final r2 = safeRadius * safeRadius;

      for (var y = 0; y < safeRadius; y++) {
        for (var x = 0; x < safeRadius; x++) {
          final corners = <(int, int, int, int)>[
            (x, y, safeRadius, safeRadius),
            (source.width - 1 - x, y, source.width - safeRadius, safeRadius),
            (x, source.height - 1 - y, safeRadius, source.height - safeRadius),
            (
              source.width - 1 - x,
              source.height - 1 - y,
              source.width - safeRadius,
              source.height - safeRadius,
            ),
          ];
          for (final corner in corners) {
            final dx = corner.$1 - corner.$3;
            final dy = corner.$2 - corner.$4;
            if (dx * dx + dy * dy > r2) {
              final pixel = output.getPixel(corner.$1, corner.$2);
              output.setPixelRgba(
                corner.$1,
                corner.$2,
                pixel.r,
                pixel.g,
                pixel.b,
                0,
              );
            }
          }
        }
      }
      return Uint8List.fromList(img.encodePng(output, level: 6));
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
