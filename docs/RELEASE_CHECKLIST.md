# Image Tools — release checklist

This checklist is for the Android release of Image Tools (`com.nexauren.imagetools`).

## Public pages
- Privacy policy: https://steep-pine-34fe.nexaurenstore.workers.dev/privacy
- Terms: https://steep-pine-34fe.nexaurenstore.workers.dev/terms
- Support and account deletion: https://steep-pine-34fe.nexaurenstore.workers.dev/support
- Confirm all three URLs open on a phone without requiring sign-in.
- Confirm the policy and store Data safety answers describe the exact release and SDK configuration. Do not claim that data is never collected if Analytics, authentication, or payment services are enabled.

## Build and signing
- Confirm GitHub Actions completes the Android build and Worker syntax check.
- For store distribution, use a signed release APK/AAB built with the production upload/release key.
- Never upload the keystore, passwords, PayPal secrets, Firebase service-account keys, or other credentials to the repository.
- Verify the package ID, version name/code, icon, and signing certificate before uploading.
- Keep the signing key and its backup in a secure location. Do not change the key between updates.

## Required device checks
- Install the release build on a clean Android 10+ device.
- Open every tool and test representative images, large images, invalid files, cancel/back navigation, and low-storage errors.
- Test resize, compression, conversion, crop, rotation, filters, watermark, collage, PDF export, and all other tools shown in the UI.
- Test export, share, and reopening saved files.
- Test Google/email sign-in, sign-out, session refresh, and account deletion request instructions.
- Test Premium purchase, entitlement refresh, cancellation, failed payment, and restoration using the configured production PayPal plan before enabling live billing.
- Check offline behavior and clear error messages when authentication or the Worker is unavailable.

## Store listing
- Add app icon, phone screenshots, short/full descriptions, support URL, and privacy policy URL.
- Complete the store's Data safety / data collection declarations from the actual SDK and backend behavior.
- Provide a contact channel monitored by the developer.
- Review target audience, content rating, permissions, ads declaration, and billing disclosures for the chosen store.

## Current known release configuration
- Version name: 1.17.0
- Version code: 39
- Minimum Android: 10 (API 29)
- Target Android: API 36

A successful CI build is not a substitute for testing on a real device or confirming production payment credentials and provider configuration.
