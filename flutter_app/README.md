# Image Tools — production Flutter application

This directory contains the **single production Android application** for Image Tools. All image tools are part of this app and are delivered together in one universal APK.

## Release identity

- Application ID: `com.nexauren.imagetools`
- Version: `1.19.0+49`
- Minimum Android: 10 (API 29)
- Target Android: API 36
- Release signing: existing GitHub Actions production keystore secrets

Do not change the production application ID or replace the existing signing key. A store update must remain compatible with the already-published application.

## Version policy

The visible version is kept at **1.19.0**. The fixed Android version code is **49** so the restored Flutter build can update a previously installed 1.19.0/2.3.0 test build. Do not increment it for routine development edits; only change it for an actual store release when an update code is required.

## Universal APK

The production workflow runs:

```bash
flutter build apk --release
```

It deliberately does **not** use `--split-per-abi`. The result is one APK containing the runtime libraries for ARMv7, ARM64, and x86_64:

`build/app/outputs/flutter-apk/image-tools-universal-release.apk`

CI checks all three ABI libraries, the package name and version, APK alignment, and the production signing certificate before uploading the artifact.

## Product structure

The home screen provides one integrated catalogue, global search, primary and advanced tools, projects, Premium, and settings. Every tool opens its own dedicated workspace; the catalogue and workspaces belong to the same application and APK.

The app supports dark and light themes, motion feedback, visible processing/export states, accessible touch targets, and back/undo behaviour. Image processing is on-device; the payment service does not receive source images.

## Build locally

Install Flutter stable and Android SDK 36, then run:

```bash
flutter pub get
flutter analyze
flutter test
flutter build apk --release
```

For production, use the **Android Build** GitHub Actions workflow so the universal APK is signed with the existing production key and checked before release.
