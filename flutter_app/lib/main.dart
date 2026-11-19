import 'package:material_ui/material_ui.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'app.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final preferences = await SharedPreferences.getInstance();
  final initialDarkMode = preferences.getBool('image_tools_dark_mode') ?? true;
  runApp(ImageToolsApp(initialDarkMode: initialDarkMode));
}
