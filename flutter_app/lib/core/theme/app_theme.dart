import 'package:flutter/material.dart';

abstract final class AppTheme {
  static const Color electricBlue = Color(0xFF168CFF);
  static const Color cyan = Color(0xFF22D3EE);
  static const Color violet = Color(0xFF8B5CF6);
  static const Color navy = Color(0xFF070D1B);
  static const Color darkSurface = Color(0xFF111C31);

  static ThemeData get dark => _build(Brightness.dark);
  static ThemeData get light => _build(Brightness.light);

  static ThemeData _build(Brightness brightness) {
    final isDark = brightness == Brightness.dark;
    final scheme = ColorScheme.fromSeed(
      seedColor: electricBlue,
      brightness: brightness,
      surface: isDark ? darkSurface : const Color(0xFFF5F7FC),
    );

    return ThemeData(
      useMaterial3: true,
      brightness: brightness,
      colorScheme: scheme,
      scaffoldBackgroundColor: isDark ? navy : const Color(0xFFF3F6FC),
      appBarTheme: AppBarTheme(
        backgroundColor: isDark ? navy : const Color(0xFFF3F6FC),
        foregroundColor: isDark ? Colors.white : const Color(0xFF10182A),
        elevation: 0,
        centerTitle: false,
      ),
      cardTheme: CardThemeData(
        color: isDark ? darkSurface : Colors.white,
        elevation: 0,
        margin: EdgeInsets.zero,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(22)),
      ),
      navigationBarTheme: NavigationBarThemeData(
        backgroundColor: isDark ? const Color(0xFF0C1528) : Colors.white,
        indicatorColor: electricBlue.withValues(alpha: 0.16),
        labelTextStyle: WidgetStateProperty.resolveWith(
          (states) => TextStyle(
            fontSize: 11,
            fontWeight: states.contains(WidgetState.selected) ? FontWeight.w700 : FontWeight.w500,
          ),
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: isDark ? const Color(0xFF17243B) : const Color(0xFFEFF3FA),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: BorderSide.none,
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: BorderSide.none,
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: const BorderSide(color: electricBlue, width: 1.4),
        ),
      ),
      sliderTheme: SliderThemeData(
        activeTrackColor: electricBlue,
        thumbColor: Colors.white,
        overlayColor: electricBlue.withValues(alpha: 0.16),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          backgroundColor: electricBlue,
          foregroundColor: Colors.white,
          minimumSize: const Size.fromHeight(54),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(17)),
          textStyle: const TextStyle(fontWeight: FontWeight.w800, fontSize: 15),
        ),
      ),
      textTheme: ThemeData(brightness: brightness).textTheme.apply(
        bodyColor: isDark ? const Color(0xFFE8F0FF) : const Color(0xFF172033),
        displayColor: isDark ? Colors.white : const Color(0xFF111827),
      ),
    );
  }
}
