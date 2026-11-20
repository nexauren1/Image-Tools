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


  static Future<Uint8List> addTextWatermark(
    Uint8List originalBytes, {
    required String text,
    double opacity = 0.72,
    String position = 'bottomRight',
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
      final font = source.width >= 1800 ? img.arial48 : img.arial24;
      final estimatedWidth = (cleanText.length * (source.width >= 1800 ? 28 : 14))
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
        color: img.ColorRgba8(255, 255, 255, safeOpacity),
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
  }) {
    return Isolate.run(() {
      if (images.length < 2 || images.length > 9) {
        throw const FormatException('Escolhe entre 2 e 9 imagens para a colagem.');
      }
      final columns = math.sqrt(images.length).ceil();
      final rows = (images.length / columns).ceil();
      final size = canvasSize.clamp(600, 2400).toInt();
      final spacing = gap.clamp(0, 48).toInt();
      final cellWidth = (size - spacing * (columns + 1)) ~/ columns;
      final cellHeight = (size - spacing * (rows + 1)) ~/ rows;
      final canvasWidth = cellWidth * columns + spacing * (columns + 1);
      final canvasHeight = cellHeight * rows + spacing * (rows + 1);
      if (canvasWidth * canvasHeight > maxPixels) {
        throw const FormatException('A colagem excede o limite de memória seguro.');
      }
      final background = img.ColorRgba8(12, 18, 33, 255);
      final canvas = img.Image(
        width: canvasWidth,
        height: canvasHeight,
        numChannels: 4,
        backgroundColor: background,
      );
      for (var index = 0; index < images.length; index++) {
        final decoded = img.decodeImage(images[index]);
        if (decoded == null) continue;
        final source = img.bakeOrientation(decoded);
        if (source.width * source.height > maxPixels) {
          throw const FormatException('Uma das imagens excede o limite seguro.');
        }
        final tile = img.copyResize(
          source,
          width: cellWidth,
          height: cellHeight,
          maintainAspect: true,
          backgroundColor: background,
          interpolation: img.Interpolation.cubic,
        );
        final column = index % columns;
        final row = index ~/ columns;
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
  }) {
    return Isolate.run(() {
      if (images.isEmpty || images.length > 60) {
        throw const FormatException('Escolhe entre 1 e 60 imagens.');
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
        output.add(_encode(source, format, quality));
      }
      if (output.isEmpty) {
        throw const FormatException('Não foi possível processar nenhuma imagem.');
      }
      return output;
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
