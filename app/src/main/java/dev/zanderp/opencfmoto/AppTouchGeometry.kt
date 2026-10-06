package dev.zanderp.opencfmoto

/** Pure coordinate scaling shared by app-mode touch injection and its unit tests. */
object AppTouchGeometry {
    fun scalePoint(
        sourceX: Int,
        sourceY: Int,
        sourceWidth: Int,
        sourceHeight: Int,
        targetWidth: Int,
        targetHeight: Int,
    ): Pair<Float, Float>? {
        if (sourceWidth <= 0 || sourceHeight <= 0 || targetWidth <= 0 || targetHeight <= 0) return null
        val x = sourceX.coerceIn(0, sourceWidth - 1).toFloat() * targetWidth / sourceWidth
        val y = sourceY.coerceIn(0, sourceHeight - 1).toFloat() * targetHeight / sourceHeight
        return x.coerceIn(0f, targetWidth - 1f) to y.coerceIn(0f, targetHeight - 1f)
    }
}
