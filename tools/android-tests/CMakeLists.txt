cmake_minimum_required(VERSION 3.22)
project(melonds_android_native_tests LANGUAGES C CXX)

if(NOT ANDROID)
    message(FATAL_ERROR "These tests run on Android. Configure with the Android NDK toolchain.")
endif()
if(NOT ANDROID_ABI MATCHES "^(arm64-v8a|x86_64)$")
    message(FATAL_ERROR "The JIT tests support Android arm64-v8a and x86_64 only.")
endif()
if(NOT ANDROID_STL STREQUAL "c++_static")
    message(FATAL_ERROR "Use ANDROID_STL=c++_static for the standalone test executable.")
endif()

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_EXPORT_COMPILE_COMMANDS ON)
get_filename_component(MELONDS_TEST_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/../.." ABSOLUTE)

# Directory scope instruments the unchanged core, teakra, and the test adapter alike.
add_compile_options(
    "$<$<COMPILE_LANGUAGE:C,CXX>:-fsanitize=undefined>"
    "$<$<COMPILE_LANGUAGE:C,CXX>:-fno-sanitize-recover=undefined>"
    "$<$<COMPILE_LANGUAGE:C,CXX>:-fno-omit-frame-pointer>"
)
add_link_options(-fsanitize=undefined -fno-sanitize-recover=undefined -static-libsan)

set(BUILD_QT_SDL OFF CACHE BOOL "No frontend in native tests" FORCE)
set(ENABLE_GDBSTUB OFF CACHE BOOL "No GDB stub in native tests" FORCE)
set(ENABLE_OGLRENDERER OFF CACHE BOOL "No OpenGL in native tests" FORCE)
set(ENABLE_JIT ON CACHE BOOL "Exercise the real JIT" FORCE)
set(ENABLE_LTO OFF CACHE BOOL "Keep diagnostics inspectable" FORCE)
set(ENABLE_LTO_RELEASE OFF CACHE BOOL "Keep diagnostics inspectable" FORCE)
set(SANITIZE "" CACHE STRING "Instrumentation is configured above for every target" FORCE)
add_subdirectory("${MELONDS_TEST_ROOT}/melonDS-android-lib" core-build EXCLUDE_FROM_ALL)

add_executable(JitInvalidationTest
    "${MELONDS_TEST_ROOT}/tools/host-tests/jit/JitInvalidationTest.cpp"
    "${MELONDS_TEST_ROOT}/tools/host-tests/jit/HostPlatform.cpp"
    "${MELONDS_TEST_ROOT}/app/src/main/cpp/FileModeString.cpp"
    AndroidDynamicLibrary.cpp
)
target_include_directories(JitInvalidationTest PRIVATE "${MELONDS_TEST_ROOT}/app/src/main/cpp")
# JIT_ENABLED and core includes come from core's PUBLIC interface. Architecture definitions
# in the core's directory do not propagate to this sibling executable's headers.
if(ANDROID_ABI STREQUAL "arm64-v8a")
    target_compile_definitions(JitInvalidationTest PRIVATE ARCHITECTURE_ARM64=1)
else()
    target_compile_definitions(JitInvalidationTest PRIVATE ARCHITECTURE_x86_64=1)
endif()
target_link_libraries(JitInvalidationTest PRIVATE core teakra ${CMAKE_DL_LIBS})

# Compile the production microphone callback against the real Android Oboe API.
# The test supplies PCM directly; it does not require microphone permission/hardware.
set(OBOE_BUILD_TESTS OFF CACHE BOOL "No Oboe test app" FORCE)
add_subdirectory("${MELONDS_TEST_ROOT}/app/src/main/cpp/oboe" oboe-build EXCLUDE_FROM_ALL)
add_executable(MicInputGainTest
    "${MELONDS_TEST_ROOT}/tools/host-tests/microphone/MicInputGainTest.cpp"
    "${MELONDS_TEST_ROOT}/app/src/main/cpp/MicInputOboeCallback.cpp"
)
target_include_directories(MicInputGainTest PRIVATE
    "${MELONDS_TEST_ROOT}/app/src/main/cpp"
    "${MELONDS_TEST_ROOT}/melonDS-android-lib/src"
)
target_compile_options(MicInputGainTest PRIVATE -fsanitize=float-cast-overflow)
target_link_libraries(MicInputGainTest PRIVATE oboe)

# The NDK legacy toolchain sets CMAKE_SYSTEM_VERSION to 1, independently of the API.
string(REGEX REPLACE "^android-" "" MELONDS_TEST_API "${ANDROID_PLATFORM}")
if(NOT MELONDS_TEST_API MATCHES "^[0-9]+$")
    message(FATAL_ERROR "Could not determine the target API from ANDROID_PLATFORM.")
endif()
file(WRITE "${CMAKE_BINARY_DIR}/android-test.properties"
    "abi=${ANDROID_ABI}\napi=${MELONDS_TEST_API}\n")
