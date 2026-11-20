import 'dart:typed_data';

import 'package:flutter/services.dart';

class ImageExporter {
  static const MethodChannel _channel = MethodChannel(
    'com.nexauren.imagetools/image_export',
  );

  static Future<String> saveToGallery({
    required Uint8List bytes,
    required String fileName,
    required String mimeType,
  }) async {
    if (bytes.isEmpty) {
      throw ArgumentError.value(bytes, 'bytes', 'A imagem está vazia.');
    }

    final uri = await _channel.invokeMethod<String>('saveImage', {
      'bytes': bytes,
      'fileName': fileName,
      'mimeType': mimeType,
    });

    if (uri == null || uri.isEmpty) {
      throw StateError('O Android não confirmou a gravação da imagem.');
    }
    return uri;
  }
}
