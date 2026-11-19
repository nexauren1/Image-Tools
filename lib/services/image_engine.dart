import 'dart:typed_data';
import 'package:flutter/foundation.dart';
import 'package:image/image.dart' as img;

class ImageInfoData {
  const ImageInfoData({
    required this.width,
    required this.height,
    required this.format,
  });

  final int width;
  final int height;
  final String format;
}

abstract final class ImageEngine {
  static Future<ImageInfoData> inspect(Uint8List bytes) async {
    final result = await compute(_inspectImage, bytes);
    return ImageInfoData(
      width: result['width']! as int,
      height: result['height']! as int,
      format: result['format']! as String,
    );
  }

  static Future<Uint8List> crop(
    Uint8List bytes, {
    required int x,
    required int y,
    required int width,
    required int height,
  }) =>
      compute(_processImage, <String, Object>{
        'bytes': bytes, 'action': 'crop', 'x': x, 'y': y,
        'width': width, 'height': height,
      });

  static Future<Uint8List> resize(
    Uint8List bytes, {
    required int width,
    required int height,
  }) =>
      compute(_processImage, <String, Object>{
        'bytes': bytes, 'action': 'resize', 'width': width, 'height': height,
      });

  static Future<Uint8List> rotate(Uint8List bytes, int degrees) =>
      compute(_processImage, <String, Object>{
        'bytes': bytes, 'action': 'rotate', 'degrees': degrees,
      });

  static Future<Uint8List> adjust(
    Uint8List bytes, {
    required int brightness,
    required double contrast,
  }) =>
      compute(_processImage, <String, Object>{
        'bytes': bytes,
        'action': 'adjust',
        'brightness': brightness,
        'contrast': contrast,
      });

  static Future<Uint8List> filter(Uint8List bytes, String filter) =>
      compute(_processImage, <String, Object>{
        'bytes': bytes, 'action': 'filter', 'filter': filter,
      });

  static Future<Uint8List> encode(
    Uint8List bytes, {
    required String format,
    required int quality,
  }) =>
      compute(_processImage, <String, Object>{
        'bytes': bytes,
        'action': 'encode',
        'format': format,
        'quality': quality,
      });
}

Map<String, Object> _inspectImage(Uint8List bytes) {
  final decoded = img.decodeImage(bytes);
  if (decoded == null) {
    throw const FormatException('Não foi possível abrir este formato de imagem.');
  }
  final image = img.bakeOrientation(decoded);
  final detected = img.findFormatForData(bytes);
  return <String, Object>{
    'width': image.width,
    'height': image.height,
    'format': detected?.name ?? 'Desconhecido',
  };
}

Uint8List _processImage(Map<String, Object> request) {
  final bytes = request['bytes']! as Uint8List;
  final decoded = img.decodeImage(bytes);
  if (decoded == null) {
    throw const FormatException('Ficheiro de imagem inválido ou não suportado.');
  }
  var image = img.bakeOrientation(decoded);
  final action = request['action']! as String;

  switch (action) {
    case 'crop':
      final x = (request['x']! as int).clamp(0, image.width - 1);
      final y = (request['y']! as int).clamp(0, image.height - 1);
      final width = (request['width']! as int).clamp(1, image.width - x);
      final height = (request['height']! as int).clamp(1, image.height - y);
      image = img.copyCrop(image, x: x, y: y, width: width, height: height);
      break;

    case 'resize':
      final width = (request['width']! as int).clamp(1, 12000);
      final height = (request['height']! as int).clamp(1, 12000);
      if (width * height > 24000000) {
        throw const FormatException('A imagem final excede o limite seguro de 24 MP.');
      }
      image = img.copyResize(
        image,
        width: width,
        height: height,
        interpolation: img.Interpolation.cubic,
      );
      break;

    case 'rotate':
      image = img.copyRotate(image, angle: request['degrees']! as int);
      break;

    case 'adjust':
      final brightness = (request['brightness']! as int).toDouble();
      final contrast = request['contrast']! as double;
      for (var y = 0; y < image.height; y++) {
        for (var x = 0; x < image.width; x++) {
          final pixel = image.getPixel(x, y);
          double tune(num value) =>
              ((value.toDouble() - 128) * contrast + 128 + brightness)
                  .clamp(0, 255)
                  .toDouble();
          image.setPixelRgba(
            x, y, tune(pixel.r), tune(pixel.g), tune(pixel.b), pixel.a,
          );
        }
      }
      break;

    case 'filter':
      final filter = request['filter']! as String;
      for (var y = 0; y < image.height; y++) {
        for (var x = 0; x < image.width; x++) {
          final pixel = image.getPixel(x, y);
          final r = pixel.r.toDouble();
          final g = pixel.g.toDouble();
          final b = pixel.b.toDouble();
          var outR = r;
          var outG = g;
          var outB = b;
          if (filter == 'grayscale') {
            final gray = 0.2126 * r + 0.7152 * g + 0.0722 * b;
            outR = gray;
            outG = gray;
            outB = gray;
          } else if (filter == 'sepia') {
            outR = (0.393 * r + 0.769 * g + 0.189 * b).clamp(0, 255);
            outG = (0.349 * r + 0.686 * g + 0.168 * b).clamp(0, 255);
            outB = (0.272 * r + 0.534 * g + 0.131 * b).clamp(0, 255);
          } else if (filter == 'negative') {
            outR = 255 - r;
            outG = 255 - g;
            outB = 255 - b;
          }
          image.setPixelRgba(
            x,
            y,
            outR.round().clamp(0, 255),
            outG.round().clamp(0, 255),
            outB.round().clamp(0, 255),
            pixel.a,
          );
        }
      }
      break;

    case 'encode':
      final format = request['format']! as String;
      final quality = (request['quality']! as int).clamp(1, 100);
      switch (format) {
        case 'JPG':
          return Uint8List.fromList(img.encodeJpg(image, quality: quality));
        case 'PNG':
          return Uint8List.fromList(img.encodePng(image, level: 6));
        case 'WEBP':
          return Uint8List.fromList(
            img.encodeWebP(image, lossless: false, quality: quality, method: 4),
          );
        case 'WEBP Lossless':
          return Uint8List.fromList(
            img.encodeWebP(image, lossless: true, quality: 100, method: 4),
          );
        default:
          throw FormatException('Formato de exportação desconhecido: ' + format);
      }

    default:
      throw FormatException('Operação desconhecida: ' + action);
  }

  // Edited images are exported to lossless PNG by default.
  return Uint8List.fromList(img.encodePng(image));
}
