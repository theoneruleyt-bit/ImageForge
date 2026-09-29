package com.imageforge.app.image

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageMathTest {
    @Test fun keepsOriginalWhenAlreadyWithinLimit() {
        assertEquals(1200 to 800, ImageMath.scaledDimensions(1200, 800, 1600))
    }

    @Test fun scalesLandscapeWithoutChangingAspectRatio() {
        assertEquals(1600 to 900, ImageMath.scaledDimensions(3840, 2160, 1600))
    }

    @Test fun scalesPortraitWithoutChangingAspectRatio() {
        assertEquals(900 to 1600, ImageMath.scaledDimensions(2160, 3840, 1600))
    }

    @Test fun nullLimitKeepsDimensions() {
        assertEquals(4032 to 3024, ImageMath.scaledDimensions(4032, 3024, null))
    }

    @Test fun invalidSourceDimensionsNeverProduceZeroSizedBitmap() {
        assertEquals(1 to 1, ImageMath.scaledDimensions(0, 0, 1080))
    }
}
