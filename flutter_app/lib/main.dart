import 'package:firebase_core/firebase_core.dart';
import 'package:material_ui/material_ui.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'app.dart';
import 'features/auth/app_session.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Firebase.initializeApp();
  final preferences = await SharedPreferences.getInstance();
  final initialDarkMode = preferences.getBool('image_tools_dark_mode') ?? true;
  final session = AppSession();
  await session.initialize();
  runApp(
    ImageToolsApp(
      initialDarkMode: initialDarkMode,
      session: session,
    ),
  );
}
