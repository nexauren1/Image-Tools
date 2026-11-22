# Image Tools

Image Tools is an all-in-one Android image studio. Its editing, conversion, export, and utility tools are delivered together in one application and one universal APK.

## Current production release

- Version: **2.2.0** (version code **47**)
- Android package: `com.nexauren.imagetools`
- Minimum Android: 10 (API 29)
- Target Android: API 36
- Production application source: `flutter_app/` (Flutter/Dart)
- Processing: on-device for image operations
- Premium payments: PayPal through the Cloudflare Worker

### One universal APK

The production workflow builds **one universal APK**, not separate APK downloads for individual processor architectures. The same APK contains the Android runtime libraries for:

- ARMv7 (`armeabi-v7a`)
- ARM64 (`arm64-v8a`)
- x86_64

Signed release output:

`flutter_app/build/app/outputs/flutter-apk/image-tools-universal-release.apk`

The workflow verifies the package name, version, all three ABI runtime libraries, APK alignment, and the existing production signing certificate before publishing the artifact. The Android application ID and production signing key are preserved so a compatible update can be installed over the existing listing.

## Integrated toolbox

The app brings its image tools together in one catalogue and one navigation system, with dedicated workspaces for individual operations.

### Image editing
- Crop with free selection and aspect-ratio presets
- Resize with dimensions and aspect-ratio controls
- Rotate and mirror
- Brightness, contrast, saturation, temperature, and tint
- Automatic enhancement and filters
- Text, watermark, and logo overlays

### File and export tools
- Compress and convert JPEG, PNG, and WebP
- Preserve image dimensions unless the user changes them
- Export image collections and batch operations
- Image to PDF
- Image to Base64
- Image information and EXIF metadata tools

### Creative and utility tools
- Collage
- Dominant colour palette
- Social-network size presets
- Face blur and OCR
- Replace colour
- Borders and rounded corners
- Blur, sharpen, pixelate, document scan, and image splitting

A tool is considered release-ready only when its workspace and processing path are implemented and tested; a catalogue card alone is not proof of a finished feature.

## Privacy and legal pages

The Cloudflare Worker serves public pages suitable for app-store listings:

- Home: `/`
- Privacy Policy: `/privacy`
- Terms of Service: `/terms`
- Support and account deletion: `/support`

Production base URL:

`https://steep-pine-34fe.nexaurenstore.workers.dev`

Use the Privacy Policy URL above in APKPure/Uptodown publisher metadata.

## PayPal

The Android app starts Premium checkout through:

`POST /paypal/create-subscription`

and verifies entitlement through:

`GET /paypal/subscription-status`

Cancellation remains available through the secure payment backend endpoint:

`POST /paypal/cancel-subscription`

The PayPal client secret stays on the Cloudflare Worker. Do not put PayPal credentials in the APK or GitHub source.

The worker supports PayPal live mode when `PAYPAL_ENVIRONMENT=live` is configured.

## Firebase

The app uses Firebase Authentication for Google and email/password sign-in and Firestore for server-managed entitlement state. Client writes to entitlement documents are disabled in Firestore rules.

## Build and release

The **Android Build** GitHub Actions workflow runs Worker tests, Flutter static analysis and tests, then builds and signs one universal release APK.

Required GitHub Actions secrets for signed releases:

- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

Optional build variables:

- `WORKER_URL`
- `GOOGLE_WEB_CLIENT_ID`

The signed artifact is named `image-tools-universal-release-apk` and contains:

`image-tools-universal-release.apk`

To build locally with Flutter:

```bash
cd flutter_app
flutter pub get
flutter build apk --release
```

Do not add `--split-per-abi` to the production build. That option creates separate architecture-specific APKs and is not the distribution format for this project.

## Publishing checklist

Before submitting to a store:

1. Confirm the Cloudflare Worker is deployed with the latest `cloudflare-worker/src/index.js`.
2. Confirm PayPal is live and the production plan is active.
3. Confirm the PayPal webhook points to `/paypal/webhook`.
4. Download and test the single signed universal APK.
5. Confirm the package ID, version code, and production signing certificate match the installed release.
6. Test sign-in, Premium checkout, return/deep-link handling, entitlement refresh, and cancellation.
7. Publish the Privacy Policy URL and provide the store listing assets.

## Security

Never commit PayPal client secrets, Firebase service-account private keys, release keystore files or passwords, or other production credentials.

The Flutter source in `flutter_app/` is the production app. The older Kotlin source in `app/src/` is retained in the repository as legacy code and is not built into the current release. The Firebase Android configuration at `app/google-services.json` is copied into the Flutter Android project by CI.

## Release history

### 2.2.0
Replaced the Android launcher icon with the supplied Image Tools artwork. Added on-device pencil sketch, vignette and outline effects, plus Noir, Cinematic, Pastel, Solarize and Posterize filter presets. Package identity and production signing certificate remain unchanged.

### 2.1.0
Unified the production distribution into one universal APK containing ARMv7, ARM64, and x86_64 runtime libraries. The application ID and existing production signing certificate remain unchanged.

### 1.19.0
Rebuilt the home screen around the Image Studio reference, added guest access, and improved visible progress during processing and export. PNG and WEBP Lossless are available for lossless output; compression starts in WEBP Lossless and uses a 95% default for lossy formats. Same-format conversion preserves the original encoded bytes. Added on-device ML Kit subject segmentation for transparent background removal; Google Play services may download the model on first use. The existing package ID is retained so this version updates the installed app.

### 1.18.0
Expanded quick collage from four to nine images, replaced the launcher identity with an adaptive Android icon, refreshed public privacy/terms/support pages, and introduced automated tests for the Worker legal pages.

### 1.17.0
Reliability improvements: persist theme preference, handle Android system Back navigation, clean up authentication/network resources, guard image allocations, reduce batch-processing memory use, prevent partial export files, preserve GIF frame proportions, and validate pull requests before release.

Public privacy policy source: [PRIVACY.md](PRIVACY.md). Worker legal-page tests run with `npm --prefix cloudflare-worker test`.
