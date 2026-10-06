# Image Tools

Modern Android image toolbox with local image processing, Firebase accounts, Firestore entitlements, and PayPal Sandbox checkout through a Cloudflare Worker.

## First release
- Resize Image
- Compress Image
- Convert Image (JPEG / PNG / WEBP)
- Google Sign-In
- Email / Password
- Firestore account profile
- Free / Premium plans
- Premium: one-time US$9
- Cloudflare Worker payment backend

## Firebase
Add Android app package com.nexauren.imagetools to the shared Firebase project.
Enable Google and Email/Password sign-in and Cloud Firestore.
Add SHA-1 and SHA-256 for the app.
Download google-services.json to app/.
Use the shared Firebase project so one user identity can work across multiple Nexauren applications.

## Build variables
Pass WORKER_URL and GOOGLE_WEB_CLIENT_ID as Gradle properties or GitHub repository variables.

## Cloudflare Worker
See cloudflare-worker/README.md. Never ship PayPal Client Secret or Firebase service-account private key inside the APK.