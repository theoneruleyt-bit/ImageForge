# ImageForge V0.4 — Smart Target Size

ImageForge is a privacy-first Android image toolkit. V0.4 adds a real exact-size workflow on top of the proven V0.3 image engine.

## V0.4
- Fit Upload Limit mode with 50 KB–2 MB target control
- Quick targets: 100 KB, 250 KB, 500 KB, 1 MB
- Iterative quality search for JPG/WebP
- Automatic dimension reduction only when quality alone cannot hit the target
- Reports final size, dimensions, format and quality used
- All processing remains on-device

PNG is intentionally excluded from exact-size solving because Bitmap PNG encoding is lossless and its quality parameter does not provide predictable target-size control.


## V0.4.1 Exact-size fix
Target KB limits now use decimal upload-limit units (1 KB = 1000 bytes), so a 100 KB target is capped at 100,000 bytes rather than 102,400 bytes. The quality search also tests quality 100 before reducing resolution.
