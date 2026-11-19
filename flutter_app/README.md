# Image Tools 2.0 — Flutter source

This directory is the new Flutter/Dart application under construction. It intentionally lives beside the current Kotlin application while the migration branch is tested.

## Target Android identity

- Application ID: `com.nexauren.imagetools`
- First candidate version: `2.0.0+44`
- Minimum Android: 10 (API 29)
- Release signing: existing GitHub Actions production keystore secrets

## Build locally

Install Flutter 3.50 or newer and Android SDK 36, then run:

```bash
cd flutter_app
flutter pub get
flutter build apk --release
```

The Android project uses the Flutter Gradle plugin. The signed release pipeline will reuse the existing production keystore; never replace it with a debug key.

## Current migration slice

- New dark/light design system.
- Reference-inspired home screen and full tool catalogue.
- Dedicated tool workspaces with local image selection.
- Interactive crop frame with touch drag, edge/corner handles, and aspect presets.
- Isolate-based crop/resize/convert/enhance operations.
- Android MediaStore export to Pictures/Image Tools.

The catalogue records the full target scope. Tools are not marked release-ready merely because their cards are visible; each engine must be implemented and tested before the migration can replace the production build.
