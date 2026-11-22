import 'package:material_ui/material_ui.dart';

enum ToolId {
  crop,
  rotate,
  resize,
  compress,
  convert,
  enhance,
  autoEnhance,
  colorGrade,
  addText,
  filters,
  collage,
  watermark,
  imageInfo,
  palette,
  imageToPdf,
  base64,
  metadata,
  faceBlur,
  ocr,
  batch,
  socialPresets,
  replaceColor,
  border,
  roundCorners,
  blur,
  sharpen,
  pixelate,
  documentScan,
  splitImage,
  sketch,
  vignette,
  outline,
}

class ToolDescriptor {
  const ToolDescriptor({
    required this.id,
    required this.title,
    required this.subtitle,
    required this.icon,
    required this.color,
    this.premium = false,
  });

  final ToolId id;
  final String title;
  final String subtitle;
  final IconData icon;
  final Color color;
  final bool premium;
}

abstract final class ToolCatalog {
  // Six high-frequency actions, matching the reference home screen.
  static const List<ToolDescriptor> primary = [
    ToolDescriptor(
      id: ToolId.crop,
      title: 'Editar imagem',
      subtitle: 'Cortar e ajustar',
      icon: Icons.crop,
      color: Color(0xFF168CFF),
    ),
    ToolDescriptor(
      id: ToolId.resize,
      title: 'Redimensionar',
      subtitle: 'Tamanho e proporção',
      icon: Icons.fit_screen,
      color: Color(0xFF7C3AED),
    ),
    ToolDescriptor(
      id: ToolId.compress,
      title: 'Comprimir',
      subtitle: 'Reduzir o tamanho',
      icon: Icons.storage_rounded,
      color: Color(0xFF16A34A),
    ),
    ToolDescriptor(
      id: ToolId.convert,
      title: 'Converter',
      subtitle: 'JPG, PNG e WebP',
      icon: Icons.autorenew_rounded,
      color: Color(0xFFF97316),
    ),
    ToolDescriptor(
      id: ToolId.enhance,
      title: 'Melhorar imagem',
      subtitle: 'Luz, cor e contraste',
      icon: Icons.auto_awesome,
      color: Color(0xFF9333EA),
    ),
    ToolDescriptor(
      id: ToolId.autoEnhance,
      title: 'Melhorar automaticamente',
      subtitle: 'Correção inteligente de luz e cor',
      icon: Icons.auto_awesome_rounded,
      color: Color(0xFF0891B2),
    ),
  ];

  static const List<ToolDescriptor> advanced = [
    ToolDescriptor(
      id: ToolId.colorGrade,
      title: 'Temperatura e tonalidade',
      subtitle: 'Ajustar tons quentes, frios e matiz',
      icon: Icons.thermostat_rounded,
      color: Color(0xFF0EA5E9),
    ),
    ToolDescriptor(
      id: ToolId.addText,
      title: 'Adicionar texto',
      subtitle: 'Títulos e legendas',
      icon: Icons.title,
      color: Color(0xFF64748B),
    ),
    ToolDescriptor(
      id: ToolId.filters,
      title: 'Filtros e efeitos',
      subtitle: 'Estilos e ajustes',
      icon: Icons.palette,
      color: Color(0xFF8B5CF6),
    ),
    ToolDescriptor(
      id: ToolId.collage,
      title: 'Colagem',
      subtitle: 'Juntar várias fotografias',
      icon: Icons.grid_view_rounded,
      color: Color(0xFF0D9488),
    ),
    ToolDescriptor(
      id: ToolId.rotate,
      title: 'Rodar e espelhar',
      subtitle: 'Orientação da imagem',
      icon: Icons.rotate_right,
      color: Color(0xFF64748B),
    ),
    ToolDescriptor(
      id: ToolId.watermark,
      title: 'Marca de água',
      subtitle: 'Texto e logótipo',
      icon: Icons.branding_watermark,
      color: Color(0xFF64748B),
    ),
    ToolDescriptor(
      id: ToolId.imageInfo,
      title: 'Detalhes da imagem',
      subtitle: 'Resolução e formato',
      icon: Icons.info_outline,
      color: Color(0xFF64748B),
    ),
    ToolDescriptor(
      id: ToolId.palette,
      title: 'Paleta de cores',
      subtitle: 'Cores dominantes',
      icon: Icons.color_lens_outlined,
      color: Color(0xFF64748B),
    ),
    ToolDescriptor(
      id: ToolId.imageToPdf,
      title: 'Imagem para PDF',
      subtitle: 'Criar documento',
      icon: Icons.picture_as_pdf_outlined,
      color: Color(0xFFDC2626),
    ),
    ToolDescriptor(
      id: ToolId.base64,
      title: 'Imagem para Base64',
      subtitle: 'Exportar como texto',
      icon: Icons.code,
      color: Color(0xFF64748B),
    ),
    ToolDescriptor(
      id: ToolId.metadata,
      title: 'Metadados EXIF',
      subtitle: 'Ver e remover dados',
      icon: Icons.location_searching,
      color: Color(0xFF64748B),
    ),
    ToolDescriptor(
      id: ToolId.faceBlur,
      title: 'Desfocar rostos',
      subtitle: 'Privacidade visual',
      icon: Icons.face_retouching_off,
      color: Color(0xFF64748B),
      premium: true,
    ),
    ToolDescriptor(
      id: ToolId.ocr,
      title: 'Extrair texto (OCR)',
      subtitle: 'Imagem para texto',
      icon: Icons.document_scanner_outlined,
      color: Color(0xFF64748B),
      premium: true,
    ),
    ToolDescriptor(
      id: ToolId.batch,
      title: 'Processamento em lote',
      subtitle: 'Várias imagens',
      icon: Icons.playlist_add_check,
      color: Color(0xFF64748B),
      premium: true,
    ),
    ToolDescriptor(
      id: ToolId.socialPresets,
      title: 'Tamanhos sociais',
      subtitle: 'Redes sociais',
      icon: Icons.share_outlined,
      color: Color(0xFF64748B),
    ),
    ToolDescriptor(
      id: ToolId.replaceColor,
      title: 'Substituir cor',
      subtitle: 'Cor seletiva',
      icon: Icons.colorize,
      color: Color(0xFF64748B),
      premium: true,
    ),
    ToolDescriptor(
      id: ToolId.border,
      title: 'Molduras e cantos',
      subtitle: 'Bordas e arredondamento',
      icon: Icons.crop_square,
      color: Color(0xFF64748B),
    ),
    ToolDescriptor(
      id: ToolId.roundCorners,
      title: 'Cantos arredondados',
      subtitle: 'PNG com cantos transparentes',
      icon: Icons.rounded_corner,
      color: Color(0xFF64748B),
    ),
    ToolDescriptor(
      id: ToolId.blur,
      title: 'Desfoque',
      subtitle: 'Controlar intensidade',
      icon: Icons.blur_on,
      color: Color(0xFF64748B),
    ),
    ToolDescriptor(
      id: ToolId.sharpen,
      title: 'Nitidez',
      subtitle: 'Realçar detalhes',
      icon: Icons.filter_center_focus,
      color: Color(0xFF64748B),
    ),
    ToolDescriptor(
      id: ToolId.pixelate,
      title: 'Pixelizar',
      subtitle: 'Efeito mosaico',
      icon: Icons.grid_4x4,
      color: Color(0xFF64748B),
    ),
    ToolDescriptor(
      id: ToolId.documentScan,
      title: 'Digitalizar documento',
      subtitle: 'Realçar papel',
      icon: Icons.document_scanner,
      color: Color(0xFF64748B),
    ),
    ToolDescriptor(
      id: ToolId.splitImage,
      title: 'Dividir imagem',
      subtitle: 'Criar segmentos',
      icon: Icons.call_split,
      color: Color(0xFF64748B),
    ),
    ToolDescriptor(
      id: ToolId.sketch,
      title: 'Esboço a lápis',
      subtitle: 'Transformar fotografia em desenho',
      icon: Icons.brush,
      color: Color(0xFF64748B),
    ),
    ToolDescriptor(
      id: ToolId.vignette,
      title: 'Vinheta',
      subtitle: 'Escurecer suavemente as margens',
      icon: Icons.blur_on,
      color: Color(0xFF64748B),
    ),
    ToolDescriptor(
      id: ToolId.outline,
      title: 'Contornos',
      subtitle: 'Destacar linhas e formas',
      icon: Icons.filter_center_focus,
      color: Color(0xFF64748B),
    ),
  ];

  static List<ToolDescriptor> get all => [...primary, ...advanced];
}
