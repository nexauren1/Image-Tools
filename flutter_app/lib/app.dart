import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'core/theme/app_theme.dart';
import 'features/home/home_screen.dart';

class ImageToolsApp extends StatefulWidget {
  const ImageToolsApp({super.key, required this.initialDarkMode});

  final bool initialDarkMode;

  @override
  State<ImageToolsApp> createState() => _ImageToolsAppState();
}

class _ImageToolsAppState extends State<ImageToolsApp> {
  late ThemeMode _themeMode = widget.initialDarkMode ? ThemeMode.dark : ThemeMode.light;

  Future<void> _setDarkMode(bool enabled) async {
    setState(() {
      _themeMode = enabled ? ThemeMode.dark : ThemeMode.light;
    });
    final preferences = await SharedPreferences.getInstance();
    await preferences.setBool('image_tools_dark_mode', enabled);
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Image Tools',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light,
      darkTheme: AppTheme.dark,
      themeMode: _themeMode,
      home: HomeScreen(
        isDarkMode: _themeMode == ThemeMode.dark,
        onThemeChanged: _setDarkMode,
      ),
    );
  }
}
