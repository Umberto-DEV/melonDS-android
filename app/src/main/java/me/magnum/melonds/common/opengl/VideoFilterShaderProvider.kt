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
        return when (filtering) {
            VideoFiltering.NONE -> ShaderProgramSource.NoFilterShader
            VideoFiltering.LINEAR -> ShaderProgramSource.LinearShader
            VideoFiltering.XBR2 -> ShaderProgramSource.xbrShader(textureScale)
            VideoFiltering.HQ2X -> ShaderProgramSource.hq2xShader(textureScale)
            VideoFiltering.HQ4X -> ShaderProgramSource.hq4xShader(textureScale)
            VideoFiltering.QUILEZ -> ShaderProgramSource.quilezShader(textureScale)
            VideoFiltering.LCD -> ShaderProgramSource.lcdShader(textureScale)
            VideoFiltering.SCANLINES -> ShaderProgramSource.scanlinesShader(textureScale)
        }
    }
}