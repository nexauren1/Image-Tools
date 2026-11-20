import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:image/image.dart' as img;
import 'package:imagetools/features/processing/image_engine.dart';

Uint8List makeTestPng() {
  final image = img.Image(width: 8, height: 6);
  for (var y = 0; y < image.height; y++) {
    for (var x = 0; x < image.width; x++) {
      image.setPixelRgba(x, y, x * 20, y * 25, 100, 255);
    }
  }
  return Uint8List.fromList(img.encodePng(image));
}

void main() {
  test('crop uses the requested source rectangle and keeps full crop resolution', () async {
    final output = await ImageEngine.cropToPng(
      makeTestPng(),
      left: 0.25,
      top: 1 / 6,
      width: 0.5,
      height: 2 / 3,
    );
    final decoded = img.decodePng(output);
    expect(decoded, isNotNull);
    expect(decoded!.width, 4);
    expect(decoded.height, 4);
  });

  test('resize changes dimensions only when explicitly requested', () async {
    final input = makeTestPng();
    final preview = await ImageEngine.decodeForPreview(input);
    expect(preview.width, 8);
    expect(preview.height, 6);

    final output = await ImageEngine.resize(
      input,
      width: 16,
      height: 12,
      format: ImageOutputFormat.png,
    );
    final decoded = img.decodePng(output);
    expect(decoded, isNotNull);
    expect(decoded!.width, 16);
    expect(decoded.height, 12);
  });

  test('lossless PNG conversion preserves pixel dimensions', () async {
    final output = await ImageEngine.convert(
      makeTestPng(),
      format: ImageOutputFormat.png,
    );
    final decoded = img.decodePng(output);
    expect(decoded, isNotNull);
    expect(decoded!.width, 8);
    expect(decoded.height, 6);
  });

  test('quarter-turn rotation swaps the output dimensions', () async {
    final source = img.Image(width: 8, height: 6);
    final input = Uint8List.fromList(img.encodePng(source));

    final output = await ImageEngine.rotate(
      input,
      quarterTurns: 1,
    );
    final decoded = img.decodePng(output);

    expect(decoded, isNotNull);
    expect(decoded!.width, 6);
    expect(decoded.height, 8);
  });

  test('adds an outer border without changing the source pixels', () async {
    final output = await ImageEngine.addBorder(
      makeTestPng(),
      thickness: 5,
      red: 20,
      green: 40,
      blue: 80,
    );
    final decoded = img.decodePng(output);
    expect(decoded, isNotNull);
    expect(decoded!.width, 18);
    expect(decoded.height, 16);
  });

  test('pixelation preserves the image dimensions', () async {
    final output = await ImageEngine.pixelate(
      makeTestPng(),
      blockSize: 3,
    );
    final decoded = img.decodePng(output);
    expect(decoded, isNotNull);
    expect(decoded!.width, 8);
    expect(decoded.height, 6);
  });

  test('social preset creates the requested output dimensions', () async {
    final output = await ImageEngine.fitSocialPreset(
      makeTestPng(),
      width: 128,
      height: 128,
    );
    final decoded = img.decodePng(output);
    expect(decoded, isNotNull);
    expect(decoded!.width, 128);
    expect(decoded.height, 128);
  });

  test('collage contains one tile per selected image', () async {
    final output = await ImageEngine.createCollage([
      makeTestPng(),
      makeTestPng(),
    ], canvasSize: 600, gap: 10);
    final decoded = img.decodePng(output);
    expect(decoded, isNotNull);
    expect(decoded!.width, greaterThan(500));
    expect(decoded.height, greaterThan(200));
  });

  test('metadata export re-encodes a same-format PNG', () async {
    final input = makeTestPng();
    final output = await ImageEngine.stripMetadataToPng(input);
    expect(output, isNotEmpty);
    final decoded = img.decodePng(output);
    expect(decoded, isNotNull);
    expect(decoded!.width, 8);
    expect(decoded.height, 6);
  });

  test('rounded corners keep the original dimensions and add alpha', () async {
    final output = await ImageEngine.roundCorners(makeTestPng(), radius: 3);
    final decoded = img.decodePng(output);
    expect(decoded, isNotNull);
    expect(decoded!.width, 8);
    expect(decoded.height, 6);
    expect(decoded.getPixel(0, 0).a, 0);
    expect(decoded.getPixel(4, 3).a, 255);
  });

  test('replaces pixels close to the selected color', () async {
    final source = img.Image(width: 4, height: 4);
    for (var y = 0; y < source.height; y++) {
      for (var x = 0; x < source.width; x++) {
        source.setPixelRgb(x, y, 255, 0, 0);
      }
    }
    final input = Uint8List.fromList(img.encodePng(source));
    final output = await ImageEngine.replaceColor(
      input,
      targetRed: 255,
      targetGreen: 0,
      targetBlue: 0,
      replacementRed: 0,
      replacementGreen: 0,
      replacementBlue: 255,
      tolerance: 10,
    );
    final decoded = img.decodePng(output);
    expect(decoded, isNotNull);
    final pixel = decoded!.getPixel(1, 1);
    expect(pixel.r.round(), 0);
    expect(pixel.b.round(), 255);
  });

  test('rejects invalid crop bounds rather than exporting a partial image', () async {
    expect(
      () => ImageEngine.cropToPng(
        makeTestPng(),
        left: 0.8,
        top: 0.8,
        width: 0.5,
        height: 0.5,
      ),
      throwsA(isA<FormatException>()),
    );
  });
}
