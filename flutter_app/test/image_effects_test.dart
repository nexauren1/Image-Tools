import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:image/image.dart' as img;
import 'package:imagetools/features/processing/image_engine.dart';

Uint8List _sampleImage() {
  final source = img.Image(width: 64, height: 48, numChannels: 4);
  for (var y = 0; y < source.height; y++) {
    for (var x = 0; x < source.width; x++) {
      source.setPixelRgba(
        x,
        y,
        (x * 5 + y * 2) % 256,
        (y * 7 + x) % 256,
        ((x ~/ 4 + y ~/ 3) * 29) % 256,
        255,
      );
    }
  }
  return Uint8List.fromList(img.encodePng(source));
}

void _expectPngKeepsDimensions(Uint8List output) {
  final decoded = img.decodeImage(output);
  expect(decoded, isNotNull);
  expect(decoded!.width, 64);
  expect(decoded.height, 48);
  expect(output.take(8), orderedEquals([137, 80, 78, 71, 13, 10, 26, 10]));
}

void main() {
  group('ImageEngine creative effects', () {
    final effects = <String, Future<Uint8List> Function(Uint8List)>{
      'sketch': (bytes) => ImageEngine.sketch(bytes, strength: 0.8),
      'vignette': (bytes) => ImageEngine.vignette(bytes, strength: 0.8),
      'outline': (bytes) => ImageEngine.outline(bytes, strength: 0.8),
    };

    for (final entry in effects.entries) {
      test('${entry.key} returns a full-size PNG', () async {
        final input = _sampleImage();
        final output = await entry.value(input);
        _expectPngKeepsDimensions(output);
        expect(output, isNot(equals(input)));
      });
    }
  });

  test('color splash preserves the selected colour and desaturates other pixels', () async {
    final source = img.Image(width: 2, height: 1);
    source.setPixelRgb(0, 0, 240, 20, 30);
    source.setPixelRgb(1, 0, 20, 60, 220);
    final input = Uint8List.fromList(img.encodePng(source));

    final output = await ImageEngine.colorSplash(
      input,
      targetRed: 230,
      targetGreen: 45,
      targetBlue: 65,
      tolerance: 35,
      strength: 1,
    );
    final decoded = img.decodePng(output)!;
    final selected = decoded.getPixel(0, 0);
    final desaturated = decoded.getPixel(1, 0);

    expect(selected.r, greaterThan(selected.g));
    expect(selected.r, greaterThan(selected.b));
    expect((desaturated.r - desaturated.g).abs(), lessThanOrEqualTo(1));
    expect((desaturated.g - desaturated.b).abs(), lessThanOrEqualTo(1));
  });

  test('duotone maps shadows and highlights to the selected colours', () async {
    final source = img.Image(width: 2, height: 1);
    source.setPixelRgb(0, 0, 0, 0, 0);
    source.setPixelRgb(1, 0, 255, 255, 255);
    final input = Uint8List.fromList(img.encodePng(source));

    final output = await ImageEngine.duotone(
      input,
      shadowRed: 0,
      shadowGreen: 0,
      shadowBlue: 255,
      highlightRed: 255,
      highlightGreen: 128,
      highlightBlue: 0,
      strength: 1,
    );
    final decoded = img.decodePng(output)!;
    final shadow = decoded.getPixel(0, 0);
    final highlight = decoded.getPixel(1, 0);

    expect(shadow.r, 0);
    expect(shadow.g, 0);
    expect(shadow.b, 255);
    expect(highlight.r, 255);
    expect(highlight.g, 128);
    expect(highlight.b, 0);
  });

  group('ImageEngine additional filter presets', () {
    for (final filter in [
      'noir',
      'cinematic',
      'pastel',
      'solarize',
      'posterize',
    ]) {
      test('$filter returns a full-size PNG', () async {
        final input = _sampleImage();
        final output = await ImageEngine.applyFilter(
          input,
          filter: filter,
          strength: 0.8,
        );
        _expectPngKeepsDimensions(output);
        expect(output, isNot(equals(input)));
      });
    }
  });
}
