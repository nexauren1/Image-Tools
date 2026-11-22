# Image Tools — production architecture and release contract

The Flutter application in `flutter_app/` is the production Android application. The migration from the earlier Kotlin/Compose implementation has been completed for the current release line. The legacy Kotlin source remains in `app/src/` for reference and is not compiled into the production APK.

## Non-negotiable release identity

- Android application ID: `com.nexauren.imagetools`
- Current version: `2.3.0+48`
- Minimum Android: 10 (API 29)
- Existing production signing key: reuse GitHub Actions secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD`
- Production source: `flutter_app/`

Do not replace the production keystore or change the application ID for a normal store update. The Firebase Android configuration remains at `app/google-services.json` and is copied to `flutter_app/android/app/google-services.json` during CI.

## One application, one universal APK

All tools are part of one Image Tools application, share its navigation and catalogue, and ship in one APK. The production build must not use `--split-per-abi`.

The single release APK must include these Android ABIs:

- `armeabi-v7a`
- `arm64-v8a`
- `x86_64`

Expected signed artifact:

`flutter_app/build/app/outputs/flutter-apk/image-tools-universal-release.apk`

The Android Build workflow validates the application ID, version name and code, all three ABI runtime libraries, alignment, and the production certificate.

## Product structure

1. One home screen with the Image Tools identity, Premium entry, feature banner, primary tool cards, advanced tools, and bottom navigation.
2. One integrated tool catalogue with dedicated workspaces for individual operations. Do not create separate APKs or companion apps for tool categories.
3. Dark and light themes, motion feedback, visible processing/export states, accessible touch targets, and undo/back behaviour.
4. Crop workspace with draggable crop rectangle, edge/corner handles, free crop, and 1:1, 4:5, 16:9, and 9:16 presets.
5. Image processing stays on-device. Source images must not be uploaded to the payment server.

## Image-quality contract

- Decode the original file at full resolution for export; preview-size images must never be used as export sources.
- Apply EXIF orientation before presenting the editable image.
- Do not change dimensions unless the user selects crop or resize.
- Do not re-encode the source for a no-op/same-format operation.
- PNG and lossless WebP remain lossless; JPEG and lossy WebP expose quality controls.
- Represent a sequence of edits as parameters against the original source, not repeated lossy exports.
- Guard pixel count and memory use, process off the UI thread, write to a temporary destination, and publish only complete outputs.

## Accounts and Premium

Keep the existing Firebase project, Firebase users, Firestore entitlement documents, Cloudflare Worker URL, PayPal product/plan, webhook, and legal/support URLs. Client integration must not bypass server-side entitlement ownership checks.

A verified Premium entitlement may be cached securely for a limited offline grace period. On reconnection, the app refreshes from the Worker/Firestore and applies cancellation/refund status. Image operations remain local and work offline.

## Release gates

1. Run Flutter static analysis and all Flutter tests.
2. Run Cloudflare Worker tests and syntax validation.
3. Build one universal release APK without `--split-per-abi`.
4. Confirm package ID, version name/code, all three ABI libraries, and APK alignment.
5. Sign with the existing production key and verify the certificate SHA-256:
   `0a710d4e508bc3eefa1d36863ebe943fe1f011331d006195d0123dd951b679ea`
6. Test installation and in-place update on a real Android 10+ device, plus large images, low-memory behaviour, cancellation, and offline use.

A successful CI build is not proof that an APK updates the installed store release. Before uploading, test an in-place update with the signed universal APK on a device.
