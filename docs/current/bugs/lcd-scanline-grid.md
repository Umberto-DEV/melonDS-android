# Anchor LCD and scanline filters to the native pixel grid

[Bug index](README.md) · [Validation](../VALIDATION.md)

**Functional impact:** Low: visual moiré and incorrect filter spacing. No CVSS/security score is assigned.

## Problem and path

LCD/scanline shader frequency was multiplied by internal render scale. At 4× this produced 1,024 horizontal cells rather than the native 256, changing the simulated panel grid and causing moiré.

## Correction

Keep LCD and scanline frequency anchored to the native combined 256×386 texture. Resampling filters XBR/HQ/Quilez still scale texel offsets with the actual bound texture.

## Evidence and limits

The retained ShaderProgramSourceTest checks scales 1, 2, 4 and 8 through the production provider, invariant LCD/scanline source, changing resampler offsets and nonpositive-scale coercion. These are source/numerical JVM assertions included in historical accepted suites, not fresh pixel acceptance or a performance measurement.

The current release identity and exact-APK smoke are separate from these
historical regression results. No new build/device run is claimed by this
documentation consolidation.

## Source and regression tests

- [ShaderProgramSource.kt](../../../app/src/main/java/me/magnum/melonds/common/opengl/ShaderProgramSource.kt)
- [ShaderProgramSourceTest.kt](../../../app/src/test/java/me/magnum/melonds/common/opengl/ShaderProgramSourceTest.kt)
