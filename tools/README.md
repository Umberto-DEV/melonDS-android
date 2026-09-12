# tools/

## HWASan opt-in (debug builds)

Build with a native sanitizer enabled (e.g. HWASan on device):

```
sh ./gradlew :app:assembleGitHubNightlyDebug -PnativeSanitize=hwaddress -Pandroid.injected.build.abi=arm64-v8a
```

Requirements: an AVD or device with an arm64 ABI running API 34+, and NDK 27+ (HWASan needs
a recent enough runtime and tag-checking support). It only applies to the `debug` build type.

Reading crashes: `adb logcat -b crash` for the HWASan report, then resolve native symbols
with `ndk-stack`, pointing it at the unstripped objects under
`app/build/intermediates/cxx/Debug/*/obj/arm64-v8a`.

On Android 14+ an HWASan-built APK can run on a regular (non-HWASan) device or emulator, but
only if it ships a `wrap.sh` that sets `LD_HWASAN=1` (see
https://developer.android.com/ndk/guides/hwasan and .../wrap-script); without it dlopen fails
with `TLS symbol "(null)" in dlopened libclang_rt.hwasan-aarch64-android.so`. When
`nativeSanitize` is set, the `debug` build type packages `tools/hwasan-resources/lib/arm64-v8a/wrap.sh`
via a `resources` source set (Android Studio only packages `.so` files straight from `lib/`)
and turns on `useLegacyPackaging`, as the guide requires. Before Android 14 this doesn't work
at all: you need an HWASan build of Android (e.g. a prebuilt Pixel image).
