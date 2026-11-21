import 'dart:isolate';

import 'package:flutter/services.dart';
import 'package:image/image.dart' as img;

abstract final class ImageIntelligence {
  static const MethodChannel _channel = MethodChannel(
    'com.nexauren.imagetools/image_intelligence',
  );

  static Future<String> recognizeText(String filePath) async {
    try {
      final text = await _channel.invokeMethod<String>(
        'recognizeText',
        {'filePath': filePath},
      );
      return text?.trim() ?? '';
    } on PlatformException catch (error) {
      throw FormatException(_modelError(error, feature: 'OCR'));
    }
  }

  static Future<FaceBlurResult> blurFaces(
    String filePath,
    Uint8List originalBytes, {
    double blurStrength = 16,
  }) async {
    final List<dynamic> rawBoxes;
    try {
      rawBoxes = await _channel.invokeListMethod<dynamic>(
            'detectFaces',
            {'filePath': filePath},
          ) ??
          const <dynamic>[];
    } on PlatformException catch (error) {
      throw FormatException(_modelError(error, feature: 'deteção de rostos'));
    }

    final boxes = rawBoxes.map((value) {
      final map = Map<Object?, Object?>.from(value as Map);
      return <int>[
        (map['left'] as num).round(),
        (map['top'] as num).round(),
        (map['right'] as num).round(),
        (map['bottom'] as num).round(),
      ];
    }).toList(growable: false);

    if (boxes.isEmpty) {
      return FaceBlurResult(
        bytes: Uint8List.fromList(originalBytes),
        faceCount: 0,
      );
    }

    final output = await Isolate.run(() {
      final decoded = img.decodeImage(originalBytes);
      if (decoded == null) {
        throw const FormatException('Não foi possível descodificar a imagem.');
      }
      final source = img.bakeOrientation(decoded);
      final result = source.clone();
      final radius = blurStrength.round().clamp(4, 40);

      for (final box in boxes) {
        final padX = ((box[2] - box[0]) * 0.22).round();
        final padY = ((box[3] - box[1]) * 0.22).round();
        final left = (box[0] - padX).clamp(0, source.width - 1).toInt();
        final top = (box[1] - padY).clamp(0, source.height - 1).toInt();
        final right = (box[2] + padX).clamp(left + 1, source.width).toInt();
        final bottom = (box[3] + padY).clamp(top + 1, source.height).toInt();

        if (right <= left || bottom <= top) continue;

        final region = img.copyCrop(
          source,
          x: left,
          y: top,
          width: right - left,
          height: bottom - top,
        );
        final blurred = img.gaussianBlur(region, radius: radius);
        img.compositeImage(result, blurred, dstX: left, dstY: top);
      }

      return Uint8List.fromList(img.encodePng(result, level: 6));
    });

    return FaceBlurResult(bytes: output, faceCount: boxes.length);
  }

  static String _modelError(
    PlatformException error, {
    required String feature,
  }) {
    final detail = error.message?.trim();
    final modelUnavailable =
        error.code.contains('MODEL_NOT_READY') ||
        (detail?.toLowerCase().contains('unavailable') ?? false) ||
        (detail?.toLowerCase().contains('download') ?? false);

    if (modelUnavailable) {
      return 'O modelo de $feature ainda não está instalado. Liga-te à Internet, tenta novamente e depois poderás usar a ferramenta no dispositivo.';
    }
    return detail?.isNotEmpty == true
        ? detail!
        : 'Não foi possível executar $feature nesta imagem.';
  }
}

class FaceBlurResult {
  const FaceBlurResult({
    required this.bytes,
    required this.faceCount,
  });

  final Uint8List bytes;
  final int faceCount;
}
