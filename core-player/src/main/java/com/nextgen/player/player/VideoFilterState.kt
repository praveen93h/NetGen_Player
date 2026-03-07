package com.nextgen.player.player

data class VideoFilterState(
    val brightness: Float = 0f,     // -1.0 to 1.0 (0 = default)
    val contrast: Float = 1f,       // 0.0 to 2.0 (1 = default)
    val saturation: Float = 1f,     // 0.0 to 2.0 (1 = default)
    val hue: Float = 0f,            // -180 to 180 degrees (0 = default)
    val gamma: Float = 1f           // 0.5 to 2.0 (1 = default)
) {
    val isDefault: Boolean
        get() = brightness == 0f && contrast == 1f && saturation == 1f && hue == 0f && gamma == 1f

    /**
     * Creates a 4x5 color matrix for brightness, contrast, and saturation adjustments.
     * This matrix can be applied to Compose's ColorMatrix or Android's ColorMatrixColorFilter.
     */
    fun toColorMatrixArray(): FloatArray {
        // Start with identity
        val matrix = floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )

        // Apply contrast: scale around 0.5
        val c = contrast
        val t = (1f - c) / 2f * 255f
        val contrastMatrix = floatArrayOf(
            c, 0f, 0f, 0f, t,
            0f, c, 0f, 0f, t,
            0f, 0f, c, 0f, t,
            0f, 0f, 0f, 1f, 0f
        )
        val result1 = multiplyColorMatrices(matrix, contrastMatrix)

        // Apply brightness: offset
        val b = brightness * 255f
        val brightnessMatrix = floatArrayOf(
            1f, 0f, 0f, 0f, b,
            0f, 1f, 0f, 0f, b,
            0f, 0f, 1f, 0f, b,
            0f, 0f, 0f, 1f, 0f
        )
        val result2 = multiplyColorMatrices(result1, brightnessMatrix)

        // Apply saturation
        val s = saturation
        val invSat = 1f - s
        val r = 0.213f * invSat
        val g = 0.715f * invSat
        val bl = 0.072f * invSat
        val satMatrix = floatArrayOf(
            r + s, g, bl, 0f, 0f,
            r, g + s, bl, 0f, 0f,
            r, g, bl + s, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        return multiplyColorMatrices(result2, satMatrix)
    }

    companion object {
        private fun multiplyColorMatrices(a: FloatArray, b: FloatArray): FloatArray {
            val result = FloatArray(20)
            for (i in 0..3) {
                for (j in 0..4) {
                    var sum = 0f
                    for (k in 0..3) {
                        sum += a[i * 5 + k] * b[k * 5 + j]
                    }
                    if (j == 4) sum += a[i * 5 + 4]
                    result[i * 5 + j] = sum
                }
            }
            return result
        }
    }
}
