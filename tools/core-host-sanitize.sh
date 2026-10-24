#!/usr/bin/env bash
# Configures and builds the "core" static library from the melonDS-android-lib submodule
# for the host (macOS), with the Qt/SDL frontend, GDB stub and OpenGL renderer switched off
# (none of them are needed just to compile the core, and turning them off keeps this fast
# and dependency-free) and ASan+UBSan instrumentation on by default.
#
# Usage:
#   tools/core-host-sanitize.sh          # -DSANITIZE=address,undefined (default)
#   tools/core-host-sanitize.sh --tsan   # -DSANITIZE=thread, separate build directory
#
# This only configures and builds the "core" target; it does not run anything, since core
# has no host-runnable entry point of its own -- see tools/host-tests.sh for that.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CORE_SRC_DIR="$REPO_ROOT/melonDS-android-lib"
CMAKE_BIN="${CMAKE_BIN:-$HOME/Library/Android/sdk/cmake/3.22.1/bin/cmake}"

SANITIZE="address,undefined"
BUILD_DIR="$REPO_ROOT/app/build/core-host"

if [[ "${1:-}" == "--tsan" ]]; then
    SANITIZE="thread"
    BUILD_DIR="$REPO_ROOT/app/build/core-host-tsan"
fi

if ! command -v "$CMAKE_BIN" >/dev/null 2>&1; then
    echo "cmake not found at $CMAKE_BIN (set CMAKE_BIN to override)" >&2
    exit 1
fi

mkdir -p "$BUILD_DIR"

"$CMAKE_BIN" \
    -S "$CORE_SRC_DIR" \
    -B "$BUILD_DIR" \
    -DBUILD_QT_SDL=OFF \
    -DENABLE_GDBSTUB=OFF \
    -DENABLE_OGLRENDERER=OFF \
    -DCMAKE_BUILD_TYPE=Debug \
    -DSANITIZE="$SANITIZE"

"$CMAKE_BIN" --build "$BUILD_DIR" --target core -- -j8
