import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

class ToolDefinition {
  const ToolDefinition({
    required this.id,
    required this.title,
    required this.subtitle,
    required this.icon,
    required this.color,
    this.premium = false,
  });

  final String id;
  final String title;
  final String subtitle;
  final IconData icon;
  final Color color;
  final bool premium;
}

const coreTools = <ToolDefinition>[
  ToolDefinition(id: 'crop', title: 'Editar e cortar', subtitle: 'Recorte livre e preciso', icon: Icons.crop_rounded, color: AppColors.blue),
  ToolDefinition(id: 'resize', title: 'Redimensionar', subtitle: 'Dimensões e proporções', icon: Icons.open_in_full_rounded, color: AppColors.violet),
  ToolDefinition(id: 'compress', title: 'Comprimir', subtitle: 'Controlar tamanho e qualidade', icon: Icons.storage_rounded, color: AppColors.green),
  ToolDefinition(id: 'convert', title: 'Converter', subtitle: 'JPG, PNG e WebP', icon: Icons.sync_alt_rounded, color: AppColors.orange),
  ToolDefinition(id: 'enhance', title: 'Melhorar imagem', subtitle: 'Luz, contraste e cor', icon: Icons.auto_awesome_rounded, color: AppColors.pink),
  ToolDefinition(id: 'remove_bg', title: 'Remover fundo', subtitle: 'Fundo transparente', icon: Icons.person_search_rounded, color: AppColors.cyan, premium: true),
];

const advancedTools = <ToolDefinition>[
  ToolDefinition(id: 'text', title: 'Texto e marca d’água', subtitle: 'Assinatura e logótipo', icon: Icons.title_rounded, color: AppColors.violet),
  ToolDefinition(id: 'filters', title: 'Filtros e efeitos', subtitle: 'Estilos e ajustes', icon: Icons.palette_outlined, color: AppColors.pink),
  ToolDefinition(id: 'collage', title: 'Colagem', subtitle: 'Composição de fotografias', icon: Icons.grid_view_rounded, color: AppColors.orange),
  ToolDefinition(id: 'blur', title: 'Desfocar rosto', subtitle: 'Privacidade em fotografias', icon: Icons.face_retouching_natural, color: AppColors.blue, premium: true),
  ToolDefinition(id: 'details', title: 'Detalhes da imagem', subtitle: 'Resolução e informação', icon: Icons.info_outline_rounded, color: AppColors.green),
  ToolDefinition(id: 'metadata', title: 'Metadados', subtitle: 'Consultar e remover dados', icon: Icons.security_rounded, color: AppColors.cyan),
  ToolDefinition(id: 'pdf', title: 'Imagem para PDF', subtitle: 'Criar documento PDF', icon: Icons.picture_as_pdf_rounded, color: AppColors.orange),
  ToolDefinition(id: 'base64', title: 'Imagem para Base64', subtitle: 'Exportar como texto', icon: Icons.code_rounded, color: AppColors.violet),
  ToolDefinition(id: 'palette', title: 'Paleta de cores', subtitle: 'Extrair cores dominantes', icon: Icons.color_lens_outlined, color: AppColors.pink),
  ToolDefinition(id: 'gif', title: 'GIF e divisão', subtitle: 'Frames e separação', icon: Icons.movie_rounded, color: AppColors.blue, premium: true),
  ToolDefinition(id: 'ocr', title: 'Extrair texto (OCR)', subtitle: 'Texto dentro da imagem', icon: Icons.document_scanner_outlined, color: AppColors.green),
  ToolDefinition(id: 'batch', title: 'Processamento em lote', subtitle: 'Várias imagens de uma vez', icon: Icons.photo_library_outlined, color: AppColors.cyan, premium: true),
];
