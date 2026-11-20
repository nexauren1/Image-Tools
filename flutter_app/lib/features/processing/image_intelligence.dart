import 'dart:isolate';
import 'dart:typed_data';

import 'package:google_mlkit_face_detection/google_mlkit_face_detection.dart';
import 'package:google_mlkit_subject_segmentation/google_mlkit_subject_segmentation.dart';
import 'package:google_mlkit_text_recognition/google_mlkit_text_recognition.dart';
import 'package:image/image.dart' as img;

abstract final class ImageIntelligence {
  static Future<String> recognizeText(String filePath) async {
    final recognizer = TextRecognizer(script: TextRecognitionScript.latin);
    try {
      final result = await recognizer.processImage(
        InputImage.fromFilePath(filePath),
      );
      return result.text.trim();
    } finally {
      await recognizer.close();
    }
  }

  static Future<Uint8List> removeBackground(
    String filePath,
    Uint8List originalBytes, {
    double threshold = 0.42,
  }) async {
    final segmenter = SubjectSegmenter(
      options: SubjectSegmenterOptions(
        enableForegroundBitmap: false,
        enableForegroundConfidenceMask: true,
        enableMultipleSubjects: SubjectResultOptions(
          enableConfidenceMask: false,
          enableSubjectBitmap: false,
        ),
      ),
    );
    try {
      final segmentation = await segmenter.processImage(
        InputImage.fromFilePath(filePath),
      );
      final mask = segmentation.foregroundConfidenceMask;
      if (mask == null || mask.isEmpty) {
        throw const FormatException(
          'O modelo não devolveu uma máscara. Confirma a transferência do modelo ML Kit e tenta novamente.',
        );
      }

      return await Isolate.run(() {
        final decoded = img.decodeImage(originalBytes);
        if (decoded == null) {
          throw const FormatException('Não foi possível descodificar a imagem.');
        }
        final source = img.bakeOrientation(decoded);
        if (mask.length != source.width * source.height) {
          throw const FormatException(
            'A resolução da máscara não corresponde à imagem original.',
          );
        }
        final result = img.Image(
          width: source.width,
          height: source.height,
          numChannels: 4,
        );
        var index = 0;
        for (var y = 0; y < source.height; y++) {
          for (var x = 0; x < source.width; x++) {
            final pixel = source.getPixel(x, y);
            final confidence = mask[index++].clamp(0.0, 1.0);
            final alpha = ((confidence - threshold) /
                    (1.0 - threshold).clamp(0.01, 1.0) *
                    255)
                .round()
                .clamp(0, 255)
                .toInt();
            result.setPixelRgba(
              x,
              y,
              pixel.r,
              pixel.g,
              pixel.b,
              alpha,
            );
          }
        }
        return Uint8List.fromList(img.encodePng(result, level: 6));
      });
    } finally {
      await segmenter.close();
    }
  }

  static Future<FaceBlurResult> blurFaces(
    String filePath,
    Uint8List originalBytes, {
    double blurStrength = 16,
  }) async {
    final detector = FaceDetector(
      options: FaceDetectorOptions(
        performanceMode: FaceDetectorMode.accurate,
        enableLandmarks: false,
        enableContours: false,
        enableClassification: false,
      ),
    );
    try {
      final faces = await detector.processImage(
        InputImage.fromFilePath(filePath),
      );
      if (faces.isEmpty) {
        return FaceBlurResult(
          bytes: Uint8List.fromList(originalBytes),
          faceCount: 0,
        );
      }

      final boxes = faces.map((face) {
        final rect = face.boundingBox;
        return <int>[
          rect.left.floor(),
          rect.top.floor(),
          rect.right.ceil(),
          rect.bottom.ceil(),
        ];
      }).toList(growable: false);

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

      return FaceBlurResult(bytes: output, faceCount: faces.length);
    } finally {
      await detector.close();
    }
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
