import 'package:material_ui/material_ui.dart';

abstract final class AppTheme {
  static const Color electricBlue = Color(0xFF168CFF);
  static const Color cyan = Color(0xFF22D3EE);
  static const Color violet = Color(0xFF8B5CF6);
  static const Color navy = Color(0xFF070D1B);
  static const Color darkSurface = Color(0xFF111C31);
  static const Color darkElevated = Color(0xFF17243B);

  static ThemeData get dark => _build(Brightness.dark);
  static ThemeData get light => _build(Brightness.light);

  static ThemeData _build(Brightness brightness) {
    final isDark = brightness == Brightness.dark;
    final scheme = ColorScheme.fromSeed(
      seedColor: electricBlue,
      brightness: brightness,
      surface: isDark ? darkSurface : const Color(0xFFF7F9FE),
    );

    final textColor = isDark ? const Color(0xFFEAF1FF) : const Color(0xFF172033);
    final mutedColor = isDark ? const Color(0xFF9CAEC9) : const Color(0xFF667085);

    return ThemeData(
      useMaterial3: true,
      brightness: brightness,
      colorScheme: scheme,
      scaffoldBackgroundColor: isDark ? navy : const Color(0xFFF3F6FC),
      fontFamily: 'sans-serif',
      appBarTheme: AppBarTheme(
        backgroundColor: isDark ? navy : const Color(0xFFF3F6FC),
        foregroundColor: isDark ? Colors.white : const Color(0xFF10182A),
        elevation: 0,
        centerTitle: false,
        titleTextStyle: TextStyle(
          color: isDark ? Colors.white : const Color(0xFF10182A),
          fontSize: 19,
          fontWeight: FontWeight.w900,
          letterSpacing: -0.45,
        ),
      ),
      cardTheme: CardThemeData(
        color: isDark ? darkSurface : Colors.white,
        elevation: 0,
        margin: EdgeInsets.zero,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(22),
        ),
      ),
      navigationBarTheme: NavigationBarThemeData(
        backgroundColor: isDark ? const Color(0xFF0C1528) : Colors.white,
        indicatorColor: electricBlue.withValues(alpha: 0.16),
        elevation: 8,
        height: 70,
        labelTextStyle: WidgetStateProperty.resolveWith(
          (states) => TextStyle(
            fontSize: 11,
            color: states.contains(WidgetState.selected) ? electricBlue : mutedColor,
            fontWeight: states.contains(WidgetState.selected)
                ? FontWeight.w800
                : FontWeight.w500,
          ),
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: isDark ? darkElevated : const Color(0xFFEFF3FA),
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
        hintStyle: TextStyle(color: mutedColor),
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
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(17),
          ),
          textStyle: const TextStyle(
            fontWeight: FontWeight.w800,
            fontSize: 15,
          ),
        ),
      ),
      textTheme: ThemeData(brightness: brightness).textTheme.apply(
            bodyColor: textColor,
            displayColor: isDark ? Colors.white : const Color(0xFF111827),
          ),
      dividerColor: isDark
          ? Colors.white.withValues(alpha: 0.08)
          : Colors.black.withValues(alpha: 0.08),
    );
  }
}
