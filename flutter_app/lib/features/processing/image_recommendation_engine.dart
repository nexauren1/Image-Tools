import 'image_engine.dart';

class ImageRecommendation {
  const ImageRecommendation({
    required this.title,
    required this.description,
    required this.toolId,
    required this.estimatedBenefit,
  });

  final String title;
  final String description;
  final String toolId;
  final String estimatedBenefit;
}

abstract final class ImageRecommendationEngine {
  static List<ImageRecommendation> forImage(ImageDetails details) {
    final recommendations = <ImageRecommendation>[];
    final megapixels = details.width * details.height / 1000000;
    final sizeMb = details.bytes / (1024 * 1024);
    final aspect = details.width / details.height;

    if (sizeMb >= 2.5) {
      recommendations.add(
        ImageRecommendation(
          title: 'Reduzir o tamanho do ficheiro',
          description: 'O ficheiro ocupa ' +
              sizeMb.toStringAsFixed(1) +
              ' MB. JPEG ou WebP a 82% costuma reduzir o peso sem alterar as dimensões.',
          toolId: 'compress',
          estimatedBenefit: 'Poupar espaço',
        ),
      );
    }

    if (details.width > 2400 || details.height > 2400) {
      recommendations.add(
        ImageRecommendation(
          title: 'Preparar para partilha',
          description: 'A imagem tem ' +
              details.width.toString() +
              ' × ' +
              details.height.toString() +
              ' px. Um preset de rede social evita enviar uma imagem maior do que o necessário.',
          toolId: 'socialPresets',
          estimatedBenefit: 'Dimensão adequada',
        ),
      );
    }

    if (details.hasAlpha && details.format != 'PNG' && details.format != 'WEBP') {
      recommendations.add(
        const ImageRecommendation(
          title: 'Preservar transparência',
          description: 'A imagem contém canal alfa. Exporta em PNG para evitar perder transparência.',
          toolId: 'convert',
          estimatedBenefit: 'Manter transparência',
        ),
      );
    }

    if (aspect >= 0.93 && aspect <= 1.07) {
      recommendations.add(
        const ImageRecommendation(
          title: 'Usar como publicação quadrada',
          description: 'A proporção já está próxima de 1:1. Podes preparar uma publicação quadrada sem cortar demasiado.',
          toolId: 'socialPresets',
          estimatedBenefit: 'Pronto para redes',
        ),
      );
    } else if (aspect > 1.45 || aspect < 0.69) {
      recommendations.add(
        const ImageRecommendation(
          title: 'Criar uma versão para stories',
          description: 'A proporção é panorâmica ou vertical. Cria uma versão 9:16 mantendo o ficheiro original.',
          toolId: 'socialPresets',
          estimatedBenefit: 'Formato vertical',
        ),
      );
    }

    if (megapixels < 0.6) {
      recommendations.add(
        const ImageRecommendation(
          title: 'Evitar ampliar em excesso',
          description: 'A resolução é baixa. Mantém o tamanho original ou amplia apenas o necessário para não perder nitidez.',
          toolId: 'enhance',
          estimatedBenefit: 'Proteger detalhes',
        ),
      );
    }

    if (recommendations.isEmpty) {
      recommendations.add(
        const ImageRecommendation(
          title: 'A imagem está pronta',
          description: 'A resolução e o tamanho não indicam uma otimização obrigatória. Mantém o original e aplica apenas o efeito desejado.',
          toolId: 'imageInfo',
          estimatedBenefit: 'Preservar qualidade',
        ),
      );
    }
    return recommendations.take(3).toList(growable: false);
  }
}
