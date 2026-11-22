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
  expect(decoded.format, img.Format.png);
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
