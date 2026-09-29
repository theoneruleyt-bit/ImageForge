# ImageForge V0.10 — Free/Pro + Lifetime Purchase

ImageForge is a privacy-first Android image toolkit focused on goal-based image preparation.

## V0.10
- Google Play Billing Library 9.1.0 integration.
- One-time Pro product: `imageforge_pro_lifetime`.
- Planned US price: **$1.99 Lifetime**; no subscription.
- Purchase acknowledgement and Restore Purchase flow.
- Free tier stays useful: core single-image tools, privacy tools, built-in recipes and batches up to 5 images.
- Pro unlocks unlimited batch processing, custom recipes, advanced workflows/future Pro tools and ad-free use.
- Play-provided localized price is shown when the product is available.

## Existing product foundation
Real JPG/PNG/WebP processing, Target KB, Batch, Before/After, Output Predictor, metadata privacy cleaner, Recipes, History, Export and Share.

## Play Console setup required
Create and activate a one-time product with the exact ID `imageforge_pro_lifetime`, configure the intended base price (US $1.99), upload the app to a Play test track, and test with license testers before production.

## V0.10.2
- Fixed Kotlin compiler compatibility by using Play Billing 9.1.0 core API without the unused billing-ktx module.


## V0.10.3 Restore Purchase Feedback Fix
- Restore Purchase now shows immediate progress feedback.
- A user-triggered restore continues after Google Play reconnects instead of silently returning.
- Restore results show a visible success, no-purchase, or error message.
- Billing UI callbacks are marshalled to the Android main thread.
