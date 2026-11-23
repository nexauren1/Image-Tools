import 'dart:typed_data';

import 'package:pdf/pdf.dart';
import 'package:pdf/widgets.dart' as pw;

abstract final class DocumentEngine {
  static Future<Uint8List> imagesToPdf(List<Uint8List> images) async {
    if (images.isEmpty || images.length > 40) {
      throw const FormatException('Escolhe entre 1 e 40 imagens para criar o PDF.');
    }
    if (images.any((bytes) => bytes.isEmpty)) {
      throw const FormatException('Uma das imagens selecionadas está vazia.');
    }

    final document = pw.Document(compress: true);
    for (final bytes in images) {
      final memoryImage = pw.MemoryImage(bytes);
      document.addPage(
        pw.Page(
          pageFormat: PdfPageFormat.a4,
          margin: const pw.EdgeInsets.all(28),
          build: (context) => pw.Center(
            child: pw.Image(
              memoryImage,
              fit: pw.BoxFit.contain,
            ),
          ),
        ),
      );
    }
    return document.save();
  }
}
