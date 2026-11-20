import 'package:material_ui/material_ui.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:imagetools/core/catalog/tool_catalog.dart';
import 'package:imagetools/core/theme/app_theme.dart';
import 'package:imagetools/features/home/home_screen.dart';

void main() {
  test('home catalogue exposes six primary actions', () {
    expect(ToolCatalog.primary, hasLength(6));
    expect(
      ToolCatalog.primary.map((tool) => tool.title),
      containsAll([
        'Editar imagem',
        'Redimensionar',
        'Comprimir',
        'Converter',
        'Melhorar imagem',
        'Remover fundo',
      ]),
    );
  });

  testWidgets('home screen renders its reference sections and navigation',
      (tester) async {
    tester.view.physicalSize = const Size(412, 915);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.dark,
        home: HomeScreen(
          isDarkMode: true,
          onThemeChanged: (_) {},
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Image Tools'), findsOneWidget);
    expect(find.text('Ferramentas principais'), findsOneWidget);
    expect(find.text('Ferramentas avançadas'), findsOneWidget);
    expect(find.text('Início'), findsOneWidget);
    expect(find.text('Projetos'), findsOneWidget);
    expect(find.text('Premium'), findsOneWidget);
    expect(find.text('Definições'), findsOneWidget);
  });
}
