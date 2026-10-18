# Image Tools

Image Tools is a modern Android toolbox for everyday image jobs, with a local-first workflow and optional Premium features.

## Current release

- Version: 1.18.0
- Version code: 40
- Package: `com.nexauren.imagetools`
- Minimum Android: 10 (API 29)
- Target Android: API 36
- Main processing: on-device
- Premium payments: PayPal through the Cloudflare Worker

## Tools

### Core tools
- Resize with aspect-ratio lock
- Compress with quality control
- Convert JPEG / PNG / WEBP
- Crop presets: 1:1, 4:5, 16:9, 9:16
- Rotate and mirror
- Grayscale, sepia and high-contrast filters
- Watermark text
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



### Release 1.18.0
Expanded quick collage from four to nine images with an automatic 2-column/3-column grid and uniform center-cropped tiles. Replaced the launcher identity with an adaptive Android icon, refreshed the public privacy/terms/support pages, added a private account-deletion contact path, and introduced automated tests for the Worker legal pages.

### Release 1.17.0
App-wide reliability pass: persist the theme preference, handle Android system Back navigation, clean up authentication/network resources, guard image allocations, reduce batch-processing memory use, prevent partial export files, preserve GIF frame proportions, and validate pull requests before release.

### Release 1.16.0
Full Image Studio redesign across the tool workspace, control cards, sliders, format selectors, status panels, export/share actions and tool-library cards. Updated light/dark palette, improved parameter discoverability and feedback, and fixed tool-specific control selection. Image-to-Base64 and CSS color palette remain available. Background removal retired. Notifications remain removed.

### Release 1.15.0
Added Image-to-Base64 and CSS color palette utilities, improved processing feedback and source-image previews, moved CPU-heavy image transformations off the UI thread, and removed background removal.


Public privacy policy source: [PRIVACY.md](PRIVACY.md). Production privacy page: https://steep-pine-34fe.nexaurenstore.workers.dev/privacy. Worker legal-page tests run with `npm --prefix cloudflare-worker test`.
