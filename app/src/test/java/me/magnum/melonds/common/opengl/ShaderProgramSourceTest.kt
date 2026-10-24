// Plain JUnit 4 test (project already depends on junit:junit via `testImplementation(libs.junit)`
// in app/build.gradle.kts -- no new dependency). Runs on the JVM, no Android framework classes
// involved anywhere in this file or in the production code it calls.
//
// Regression coverage for the moire fix in ShaderProgramSource: lcdShader/scanlinesShader simulate
// the physical LCD panel and must stay anchored to the native 256x386 texture size regardless of
// the internal render scale, while the resampling filters (xbr2/hq2x/hq4x/quilez) must keep scaling
// their texel offsets with the bound texture's real size.

package me.magnum.melonds.common.opengl

import me.magnum.melonds.domain.model.VideoFiltering
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShaderProgramSourceTest {

    companion object {
        private val SCALES = listOf(1, 2, 4, 8)
    }

    @Test
    fun `lcd shader omega stays anchored to native resolution at every scale`() {
        val referenceVertex = ShaderProgramSource.lcdShader().vertexShaderSource

        for (scale in SCALES) {
            // lcdShader() no longer takes a scale, but we still exercise it through the provider
            // (the actual call site) to make sure the scale argument has no effect on its output.
            val source = VideoFilterShaderProvider.getShaderSource(VideoFiltering.LCD, scale)

            assertEquals(referenceVertex, source.vertexShaderSource)
            assertTrue("scale=$scale should keep native width 256", source.vertexShaderSource.contains("vec2(256"))
            assertTrue("scale=$scale should keep native height 386", source.vertexShaderSource.contains("256, 386"))
            assertFalse("scale=$scale must not scale up to 1024", source.vertexShaderSource.contains("vec2(1024"))
            assertFalse("scale=$scale must not scale up to 2048", source.vertexShaderSource.contains("vec2(2048"))
        }
    }

    @Test
    fun `scanlines shader omega stays anchored to native resolution at every scale`() {
        val referenceVertex = ShaderProgramSource.scanlinesShader().vertexShaderSource

        for (scale in SCALES) {
            val source = VideoFilterShaderProvider.getShaderSource(VideoFiltering.SCANLINES, scale)

            assertEquals(referenceVertex, source.vertexShaderSource)
            assertTrue("scale=$scale should keep native width 256", source.vertexShaderSource.contains("vec2(256"))
            assertTrue("scale=$scale should keep native height 386", source.vertexShaderSource.contains("256, 386"))
            assertFalse("scale=$scale must not scale up to 1024", source.vertexShaderSource.contains("vec2(1024"))
            assertFalse("scale=$scale must not scale up to 2048", source.vertexShaderSource.contains("vec2(2048"))
        }
    }

    @Test
    fun `resampling filters scale texel offsets with the bound texture size`() {
        val scale = 4
        val expectedWidth = 256 * scale
        val expectedHeight = 386 * scale

        val filtersWithScaledOffsets = listOf(
            ShaderProgramSource.xbrShader(scale).vertexShaderSource,
            ShaderProgramSource.hq2xShader(scale).vertexShaderSource,
            ShaderProgramSource.hq4xShader(scale).vertexShaderSource,
            ShaderProgramSource.quilezShader(scale).fragmentShaderSource,
        )

        for (source in filtersWithScaledOffsets) {
            assertTrue("expected scaled width $expectedWidth in: $source", source.contains(expectedWidth.toString()))
            assertTrue("expected scaled height $expectedHeight in: $source", source.contains(expectedHeight.toString()))
            assertFalse("must not fall back to the native-only 256x386 size", source.contains("vec2(256, 386)"))
        }
    }

    @Test
    fun `resampling filters produce different texel offsets across scales`() {
        for (scale in SCALES) {
            val expectedWidth = 256 * scale
            val expectedHeight = 386 * scale

            assertTrue(ShaderProgramSource.xbrShader(scale).vertexShaderSource.contains(expectedWidth.toString()))
            assertTrue(ShaderProgramSource.xbrShader(scale).vertexShaderSource.contains(expectedHeight.toString()))
        }
    }

    @Test
    fun `provider coerces a non-positive scale to 1 instead of dividing by zero`() {
        val zeroScaleSource = VideoFilterShaderProvider.getShaderSource(VideoFiltering.XBR2, 0)
        val oneScaleSource = VideoFilterShaderProvider.getShaderSource(VideoFiltering.XBR2, 1)

        assertEquals(oneScaleSource.vertexShaderSource, zeroScaleSource.vertexShaderSource)
        assertFalse(zeroScaleSource.vertexShaderSource.contains("vec2(0, 0)"))
    }
}
