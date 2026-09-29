# ImageForge 1.0 — Google Play Release Pack

## App identity
- App name: ImageForge – Image Toolkit
- Application ID: `com.imageforge.app`
- Version: 1.0.0
- Version code: 18
- Monetization: useful Free tier + one-time Pro unlock
- Play product ID: `imageforge_pro_lifetime`
- Intended US base price: $1.99 Lifetime; Google Play localized ProductDetails is authoritative in-app.

## Store positioning
ImageForge prepares images for real upload goals instead of exposing only generic compression controls. Users can compress, resize, convert, target a file-size limit, process batches, clean supported metadata, compare before/after results, save recipes, and export/share results.

## Suggested short description
Compress, resize, convert and prepare images for upload — privately on your device.

## Suggested full description
ImageForge is an on-device image toolkit built around practical goals.

Choose what you need to do: make a file smaller, fit an upload limit, resize an image, convert JPG/PNG/WebP, prepare multiple images, or remove supported photo metadata. ImageForge shows useful output information and keeps core image processing on your device.

Key features:
• Smart image compression and resizing
• Target file-size workflows
• JPG, PNG and WebP conversion
• Batch processing
• Before/after comparison and output estimates
• Metadata viewer and privacy cleaning
• Reusable recipes and custom workflows
• Local processing history, export and sharing

The Free version remains useful. ImageForge Pro is a one-time Lifetime unlock for advanced workflows such as unlimited batch processing and Pro automation. No subscription is required.

## Play Console billing setup
Create a one-time product with the exact product ID `imageforge_pro_lifetime`. Configure the intended US base price as $1.99 and review Play's localized prices. Activate the product before purchase testing.

## Release sequence
1. Create the app in Play Console with application ID `com.imageforge.app`.
2. Complete app content, data safety, content rating, target audience and privacy-policy fields truthfully from the final build.
3. Create and activate `imageforge_pro_lifetime` as a one-time product.
4. Create an upload key and configure Play App Signing. Keep keystore files/passwords out of Git.
5. Produce a signed release AAB locally or in a protected CI environment.
6. Upload to Internal testing first and add the tester Google account.
7. Install ImageForge from the Play test track — not by sideloading — for the real Billing test.
8. Verify $1.99/localized product display, purchase, acknowledgement, Pro entitlement, app restart, reinstall and Restore purchase.
9. Run final image-processing smoke tests on the Play-distributed build.
10. Only then promote toward production.

## Required visual assets before production
Prepare the Play icon, feature graphic, phone screenshots, and any tablet screenshots required by the selected device distribution. Screenshots should show actual ImageForge UI and should not promise unsupported capabilities.
