# Image Tools

Modern Android image workspace focused on fast, local editing.

## Version 1.1
- Resize Image with precise dimensions and aspect-ratio lock
- Compress with JPEG / WEBP quality control
- Convert between JPEG, PNG and WEBP
- Smart Crop presets: 1:1, 4:5, 16:9 and 9:16
- Rotate & Flip
- Quick Filters: original, grayscale, sepia and high contrast
- Image Details: resolution and source file size
- Redesigned tool workspaces with a distinct visual identity per tool
- Cleaner account and sign-in experience with user-friendly error messages
- Image processing moved off the UI thread for smoother interaction

## Firebase
Android package: `com.nexauren.imagetools`.
Use the shared Firebase project. Enable Google and Email/Password sign-in and Cloud Firestore.
Register the release certificate SHA-1 and SHA-256 in Firebase before testing Google Sign-In.

## GitHub Actions release signing
Store these values as **GitHub Actions Secrets**, not public repository variables:

- `KEYSTORE_BASE64` — Base64 content of the release keystore
- `KEYSTORE_PASSWORD` — keystore password
- `KEY_ALIAS` — release key alias
- `KEY_PASSWORD` — release key password

The workflow uses these secrets only during the release build and creates `app-release.apk`.
If the secrets are not present, a local release build falls back to the debug signing key instead of exposing credentials.

Optional build values:
- `WORKER_URL`
- `GOOGLE_WEB_CLIENT_ID`

Never place PayPal client secrets or Firebase service-account private keys inside the APK.

## Privacy
Current image editing workflows run locally on the device. The app UI intentionally avoids exposing internal identifiers, certificate fingerprints, backend URLs, stack traces, or development diagnostics to end users.