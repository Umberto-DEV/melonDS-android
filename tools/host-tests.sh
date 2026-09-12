#!/usr/bin/env bash
# Compiles and runs the standalone host-side C++ unit tests that live next to their
# production code (RtcSyncTest.cpp, AudioOutputPolicyTest.cpp). These have zero
# Android/JNI/melonDS dependencies on purpose, so they can be built and run with a plain
# host compiler -- no NDK, no emulator, no device.
#
# Usage: tools/host-tests.sh
# Exit code: 0 if every test built and passed, non-zero otherwise.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CPP_DIR="$REPO_ROOT/app/src/main/cpp"
BUILD_DIR="$REPO_ROOT/build/host-tests"

mkdir -p "$BUILD_DIR"

CXX="${CXX:-clang++}"
CXXFLAGS=(-std=c++17 -Wall -Wextra -fsanitize=address,undefined -g -O1)

TEST_NAMES=()
TEST_RESULTS=()
OVERALL_STATUS=0

run_test() {
    local name="$1"
    shift
    local binary="$BUILD_DIR/$name"

    echo "== Compiling $name =="
    if ! "$CXX" "${CXXFLAGS[@]}" -o "$binary" "$@"; then
        echo "== $name: BUILD FAILED =="
        TEST_NAMES+=("$name")
        TEST_RESULTS+=("BUILD FAILED")
        OVERALL_STATUS=1
        return
    fi

    echo "== Running $name =="
    if "$binary"; then
        TEST_NAMES+=("$name")
        TEST_RESULTS+=("PASS")
    else
        TEST_NAMES+=("$name")
        TEST_RESULTS+=("FAIL")
        OVERALL_STATUS=1
    fi
    echo
}

run_test "RtcSyncTest" "$CPP_DIR/RtcSync.cpp" "$CPP_DIR/RtcSyncTest.cpp"
run_test "AudioOutputPolicyTest" "$CPP_DIR/AudioOutputPolicy.cpp" "$CPP_DIR/AudioOutputPolicyTest.cpp"

echo "== Summary =="
for i in "${!TEST_NAMES[@]}"; do
    printf '%-24s %s\n' "${TEST_NAMES[$i]}" "${TEST_RESULTS[$i]}"
done

exit "$OVERALL_STATUS"
