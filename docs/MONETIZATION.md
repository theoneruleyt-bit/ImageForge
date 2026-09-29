# ImageForge Monetization — V0.10

## Model
ImageForge uses a useful Free tier plus a one-time **ImageForge Pro Lifetime** purchase. There is no subscription.

- Planned US base price: **$1.99 one-time**
- Play Console product ID: `imageforge_pro_lifetime`
- Product type: one-time product / INAPP
- The app displays Google Play's localized formatted price when available.

## Free
- Single-image compression, resize and JPG/PNG/WebP conversion
- Target KB and basic presets
- Before/After and Output Predictor
- Privacy metadata viewer/removal
- Batch processing up to 5 images
- Built-in recipes

## Pro Lifetime
- Unlimited batch processing
- Custom reusable recipes
- Advanced workflows and future Pro tools
- Batch privacy automation
- Ad-free experience

## Billing behavior
The client queries current one-time purchases on connection and on Restore Purchase, acknowledges completed purchases, and caches the last known entitlement for offline continuity. Before production release, the one-time product must be created and activated in Google Play Console with the exact product ID above and tested through a Play test track.

Google recommends backend purchase-token verification for stronger fraud protection. ImageForge V0.10 keeps the utility serverless and uses Google Play client ownership queries plus acknowledgement; this tradeoff should be reviewed again before V1.0 production release.
