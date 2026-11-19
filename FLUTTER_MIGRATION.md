# Image Tools 2.0 — Flutter migration

## Non-negotiable release identity

- Android application ID: `com.nexauren.imagetools`
- Existing signing key: reuse the GitHub Actions secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD`
- Current production line: Kotlin/Compose remains untouched on `main` while Flutter is built and tested on `flutter-rebuild`
- First Flutter release candidate: version `2.0.0+42` (must be increased if a higher versionCode is released first)

The old APKPure listing is not deleted. A Flutter APK with the same application ID and the same signing certificate is delivered as an update to that listing. The Kotlin implementation is removed only after the Flutter replacement passes the device and release checklist.

## Product structure

The supplied reference image is the design target:

1. Home screen with the Image Tools identity, Premium entry, a feature banner, six primary tool cards, advanced tool cards, and bottom navigation.
2. Dedicated workspace for every tool; do not route all tools to one generic settings page.
3. Dark and light themes, motion feedback, visible processing/export states, accessible touch targets, and undo/back behaviour.
4. The crop workspace uses a draggable crop rectangle with corner and edge handles, free crop, and 1:1, 4:5, 16:9, and 9:16 presets.
5. Image processing stays on-device. The app must not upload source images to the payment server.

## Image-quality contract

- Decode the original file at full resolution for export; preview-size images must never be used as export sources.
- Apply EXIF orientation before presenting the editable image.
- Do not change dimensions unless the user selects a crop or resize operation.
- Do not re-encode the source for a no-op/same-format operation.
- PNG and lossless WebP remain lossless; JPEG and lossy WebP expose quality controls.
- A sequence of edits is represented as parameters against the original source, not repeated lossy exports.
- Guard pixel count and memory use, process off the UI thread, write to a temporary destination, and publish only complete outputs.

## Existing features to migrate

Core: crop, resize, compress, format conversion, rotation/mirror, image enhancement and filters, text/logo watermark, image information.

Advanced: background removal, collage (2–9 images), image-to-PDF, dominant colour palette, Base64 export, EXIF inspection/removal, face blur, OCR, social-network presets, batch processing, colour replacement, borders/rounded corners, blur, sharpen, pixelate, document scan, split image, and GIF/modern-format support where the target Android version supports it.

## Accounts and Premium

Keep the existing Firebase project, Firebase users, Firestore entitlement documents, Cloudflare Worker URL, PayPal product/plan, webhook, and legal/support URLs. Port the client integration to Flutter without changing server-side ownership checks.

A verified Premium entitlement is cached securely for a limited offline grace period. On reconnection, the app refreshes from the Worker/Firestore and applies cancellation/refund status. Image operations themselves remain local and work offline.

## Migration gates

1. Flutter project and Android identity build.
2. Home and crop vertical slice.
3. Independent tools and image engine.
4. Firebase sign-in, PayPal checkout, entitlement refresh, offline grace.
5. Automated tests for image dimensions, crop bounds, alpha, output format, and repeated edits.
6. Real-device tests on Android 10+ including large files, low memory, cancellation, offline use, and signed update over the existing release.
7. Only after all gates pass: replace the old Android build workflow and retire Kotlin application source.

A successful CI build is not proof that an APK can update the installed APKPure release. Before uploading, compare the new APK signing certificate with the production certificate and test an in-place update on a device.
