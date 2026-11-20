import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:image/image.dart' as img;
import 'package:imagetools/features/processing/document_engine.dart';

void main() {
  test('creates a multi-page PDF from image bytes', () async {
    final first = Uint8List.fromList(
      img.encodePng(img.Image(width: 16, height: 12)),
    );
    final second = Uint8List.fromList(
      img.encodePng(img.Image(width: 10, height: 18)),
    );

    final pdf = await DocumentEngine.imagesToPdf([first, second]);

    expect(pdf.length, greaterThan(300));
    expect(String.fromCharCodes(pdf.take(4)), '%PDF');
  });

  test('rejects an empty image collection', () async {
    expect(
      () => DocumentEngine.imagesToPdf([]),
      throwsA(isA<FormatException>()),
    );
  });
}
