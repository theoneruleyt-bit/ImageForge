# ImageForge 1.0 Release Checklist

## Source/CI
- [x] Version set to 1.0.0 / versionCode 18
- [x] CI runs JVM unit tests
- [x] CI builds debug APK
- [x] CI attempts a release AAB candidate
- [ ] V1.0 GitHub Actions green
- [ ] V1.0 phone smoke test passed

## Signing
- [ ] Create a private upload keystore outside Git
- [ ] Enroll/configure Play App Signing
- [ ] Configure release signing without committing secrets
- [ ] Produce final signed AAB

## Play Billing
- [ ] Create one-time product `imageforge_pro_lifetime`
- [ ] Set intended US base price to $1.99 and review localized prices
- [ ] Activate product
- [ ] Upload build to Internal testing
- [ ] Test purchase from Play-distributed build
- [ ] Verify purchase acknowledgement
- [ ] Verify Pro persists after restart
- [ ] Verify entitlement after reinstall
- [ ] Verify Restore purchase

## Store/compliance
- [ ] Final app icon and feature graphic
- [ ] Final phone screenshots
- [ ] Publish reviewed privacy policy at public URL
- [ ] Complete Data safety from exact final behavior/dependencies
- [ ] Complete content rating
- [ ] Complete target audience/app content declarations
- [ ] Add support contact details
- [ ] Review store text and translations

## Final quality gate
- [ ] Photo Picker
- [ ] Compress/resize/convert
- [ ] Target-size workflow
- [ ] Batch Free limit and Pro gating
- [ ] Before/After
- [ ] Metadata viewer/removal
- [ ] Recipes
- [ ] History/export/share
- [ ] Billing purchase + restore
- [ ] No startup or processing crashes on test phone
