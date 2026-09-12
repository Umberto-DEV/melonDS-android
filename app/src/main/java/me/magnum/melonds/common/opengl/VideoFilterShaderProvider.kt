package me.magnum.melonds.common.opengl

import me.magnum.melonds.domain.model.VideoFiltering

object VideoFilterShaderProvider {
    /**
     * @param textureScale Scale factor of the composited screen texture currently bound by the core
     * (1 for the Software renderer, [me.magnum.melonds.domain.model.RendererConfiguration.resolutionScaling]
     * for OpenGL/Compute). Filters that reason about neighbouring texel offsets need this to stay
     * aligned with the texture's real size instead of assuming the native 256x386 one.
     */
    fun getShaderSource(filtering: VideoFiltering, textureScale: Int): ShaderProgramSource {
        val scale = textureScale.coerceAtLeast(1)
        return when (filtering) {
            VideoFiltering.NONE -> ShaderProgramSource.NoFilterShader
            VideoFiltering.LINEAR -> ShaderProgramSource.LinearShader
            VideoFiltering.XBR2 -> ShaderProgramSource.xbrShader(scale)
            VideoFiltering.HQ2X -> ShaderProgramSource.hq2xShader(scale)
            VideoFiltering.HQ4X -> ShaderProgramSource.hq4xShader(scale)
            VideoFiltering.QUILEZ -> ShaderProgramSource.quilezShader(scale)
            // LCD and Scanlines simulate the physical panel at native resolution; they don't take a scale.
            VideoFiltering.LCD -> ShaderProgramSource.lcdShader()
            VideoFiltering.SCANLINES -> ShaderProgramSource.scanlinesShader()
        }
    }
}