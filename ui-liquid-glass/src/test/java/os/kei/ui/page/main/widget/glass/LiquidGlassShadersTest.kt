package os.kei.ui.page.main.widget.glass

import androidx.compose.ui.geometry.Size
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LiquidGlassShadersTest {
    @Test
    fun validUniformsPreserveRequestedValues() {
        val uniforms =
            resolveRadialRefractionUniforms(
                size = Size(200f, 100f),
                padding = 12f,
                centerX = 80f,
                centerY = 40f,
                radius = 48f,
                strength = 12f,
            )

        assertEquals(
            RadialRefractionUniforms(
                offset = -12f,
                centerX = 80f,
                centerY = 40f,
                radius = 48f,
                strength = 12f,
            ),
            uniforms,
        )
    }

    @Test
    fun outOfBoundsUniformsClampToComponentGeometry() {
        val uniforms =
            assertNotNull(
                resolveRadialRefractionUniforms(
                    size = Size(3f, 4f),
                    padding = 5f,
                    centerX = -20f,
                    centerY = 40f,
                    radius = 80f,
                    strength = 160f,
                ),
            )

        assertEquals(-5f, uniforms.offset)
        assertEquals(0f, uniforms.centerX)
        assertEquals(4f, uniforms.centerY)
        assertEquals(5f, uniforms.radius)
        assertEquals(5f, uniforms.strength)
        assertTrue(
            listOf(
                uniforms.offset,
                uniforms.centerX,
                uniforms.centerY,
                uniforms.radius,
                uniforms.strength,
            ).all(Float::isFinite),
        )
    }

    @Test
    fun invalidSizeSkipsEffect() {
        val invalidSizes =
            listOf(
                Size.Zero,
                Size(-1f, 10f),
                Size(10f, -1f),
                Size(Float.NaN, 10f),
                Size(10f, Float.POSITIVE_INFINITY),
                Size.Unspecified,
            )

        invalidSizes.forEach { size ->
            assertNull(validUniforms(size = size), "Expected invalid size to be rejected: $size")
        }
    }

    @Test
    fun nonFiniteInputsSkipEffect() {
        listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY).forEach { invalid ->
            assertNull(validUniforms(padding = invalid))
            assertNull(validUniforms(centerX = invalid))
            assertNull(validUniforms(centerY = invalid))
            assertNull(validUniforms(radius = invalid))
            assertNull(validUniforms(strength = invalid))
        }
    }

    @Test
    fun nonPositiveRadiusAndStrengthSkipEffect() {
        assertNull(validUniforms(radius = 0f))
        assertNull(validUniforms(radius = -1f))
        assertNull(validUniforms(radius = Float.MIN_VALUE))
        assertNull(validUniforms(strength = 0f))
        assertNull(validUniforms(strength = -1f))
        assertNull(validUniforms(strength = Float.MIN_VALUE))
    }

    @Test
    fun paddingOutsideComponentDiagonalSkipsEffect() {
        assertNull(validUniforms(padding = -1f))
        assertNull(validUniforms(size = Size(3f, 4f), padding = 5.01f))
    }

    private fun validUniforms(
        size: Size = Size(200f, 100f),
        padding: Float = 12f,
        centerX: Float = 80f,
        centerY: Float = 40f,
        radius: Float = 48f,
        strength: Float = 12f,
    ): RadialRefractionUniforms? =
        resolveRadialRefractionUniforms(
            size = size,
            padding = padding,
            centerX = centerX,
            centerY = centerY,
            radius = radius,
            strength = strength,
        )
}
