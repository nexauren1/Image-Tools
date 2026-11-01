# Image Tools

Image Tools is a modern Android toolbox for everyday image jobs, with a local-first workflow and optional Premium features.

## Current release

- Version: 1.21.0
- Version code: 43
- Package: `com.nexauren.imagetools`
- Minimum Android: 10 (API 29)
- Target Android: API 36
- Main processing: on-device
- Premium payments: PayPal through the Cloudflare Worker

## Tools

### Core tools
- Resize with aspect-ratio lock
- Compress with quality control
- Convert JPEG / PNG / WEBP / WEBP Lossless
- Preserve original encoded bytes when converting to the same format
- Export edited images to lossless PNG by default
- Compress with lossless WEBP or high-quality JPEG/WEBP
- Crop presets: 1:1, 4:5, 16:9, 9:16
- Rotate and mirror
- Grayscale, sepia and high-contrast filters
- Watermark text and transparent logo watermark
- Selective colour replacement with adjustable tolerance
- Before/after comparison before export
- Capture a full-resolution photo directly into the editing workflow
- Image details

### Additional tools
- Brightness
- Contrast
- Saturation
- Warmth
- Negative / invert
- Blur
- Sharpen
- Pixelate
- Border
- Rounded corners
- Remove common embedded metadata by re-encoding
- On-device background removal with transparent PNG export (Google Play services model download may be needed on first use)
- Image to PDF
- Dominant color palette
- Quick collage for 2–9 images
- Image to Base64 data URL with TXT export

The expanded toolbox follows common patterns found in modern image utility apps—grouped tools, batch-oriented workflows, privacy/metadata controls, collage/stitching concepts and utility exports—while keeping the implementation lightweight and original.

## Privacy and legal pages

The Cloudflare Worker serves public pages suitable for app-store listings:

- Home: `/\`
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

The Android application does not expose an in-app cancellation button in the release build.

The PayPal client secret stays on the Cloudflare Worker. Do not put PayPal credentials in the APK or GitHub source.

The worker supports PayPal live mode when `PAYPAL_ENVIRONMENT=live` is configured.

## Firebase

The app uses Firebase Authentication for Google and email/password sign-in and Firestore for server-managed entitlement state.

Client writes to the entitlement documents are disabled in Firestore rules.

## Build and release

GitHub Actions builds the signed release APK on pushes to `main` and `v*`.

Required GitHub Actions secrets for signed releases:

- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

Optional build variables:

- `WORKER_URL`
- `GOOGLE_WEB_CLIENT_ID`

The Android workflow produces:

`app/build/outputs/apk/release/app-release.apk`

## Publishing checklist

Before submitting to a store:

1. Confirm the Cloudflare Worker is deployed with the latest `cloudflare-worker/src/index.js`.
2. Confirm PayPal is live and the production plan is active.
3. Confirm the PayPal webhook points to `/paypal/webhook`.
4. Build and test the signed release APK.
5. Test sign-in, Premium checkout, return/deep-link handling, entitlement refresh and cancellation.
6. Publish the Privacy Policy URL.
7. Add the application icon, screenshots, short description and full description.
8. Upload the signed APK to the selected store.

## Security

Never commit:

- PayPal client secrets
- Firebase service-account private keys
- Release keystore files or passwords
- Other production credentials



### Release 1.21.0
Adds transparent logo watermarking with size, opacity and position controls; selective colour replacement using #RRGGBB and tolerance; side-by-side original/result comparison; and direct camera capture into a tool workspace. Back navigation is prevented while a tool is processing or saving. The new tools use the existing on-device bitmap pipeline and lossless PNG export. The existing package ID is retained so this version updates 1.20.0 installations.

### Release 1.20.0
Improved navigation with a real back stack, contextual page headers, bottom navigation only on top-level destinations, preserved tool-library filters and batch selections, and a visible four-step workflow inside each tool.

### Release 1.19.0
Rebuilt the home screen around the Image Studio reference, added guest access, and improved visible progress during processing and export. PNG and WEBP Lossless are available for lossless output; compression starts in WEBP Lossless and uses a 95% default for lossy formats. Same-format conversion preserves the original encoded bytes. Added on-device ML Kit subject segmentation for transparent background removal; Google Play services may download the model on first use. The existing package ID is retained so this version updates the installed app.

### Release 1.18.0
Expanded quick collage from four to nine images with an automatic 2-column/3-column grid and uniform center-cropped tiles. Replaced the launcher identity with an adaptive Android icon, refreshed the public privacy/terms/support pages, added a private account-deletion contact path, and introduced automated tests for the Worker legal pages.

### Release 1.17.0
App-wide reliability pass: persist the theme preference, handle Android system Back navigation, clean up authentication/network resources, guard image allocations, reduce batch-processing memory use, prevent partial export files, preserve GIF frame proportions, and validate pull requests before release.

### Release 1.16.0
Full Image Studio redesign across the tool workspace, control cards, sliders, format selectors, status panels, export/share actions and tool-library cards. Updated light/dark palette, improved parameter discoverability and feedback, and fixed tool-specific control selection. Image-to-Base64 and CSS color palette remain available. Background removal retired. Notifications remain removed.

### Release 1.15.0
Added Image-to-Base64 and CSS color palette utilities, improved processing feedback and source-image previews, moved CPU-heavy image transformations off the UI thread, and removed background removal.


Public privacy policy source: [PRIVACY.md](PRIVACY.md). Production privacy page: https://steep-pine-34fe.nexaurenstore.workers.dev/privacy. Worker legal-page tests run with `npm --prefix cloudflare-worker test`.
