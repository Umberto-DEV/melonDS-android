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
