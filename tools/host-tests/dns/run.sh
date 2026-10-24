#!/usr/bin/env bash
# Configures, builds and runs dns_override_test against the real Net_Slirp.cpp, using
# the Android SDK's bundled CMake (matches what the app build uses).
#
# Usage: tools/host-tests/dns/run.sh [--clean]
#   --clean   remove the build directory (app/build/host-dns) after running.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
BUILD_DIR="$REPO_ROOT/app/build/host-dns"

CMAKE="${CMAKE:-$HOME/Library/Android/sdk/cmake/3.22.1/bin/cmake}"
if [ ! -x "$CMAKE" ]; then
    echo "cmake not found at $CMAKE (set CMAKE=... to override)" >&2
    exit 1
fi

CLEAN=0
if [ "${1:-}" = "--clean" ]; then
    CLEAN=1
fi

cleanup() {
    if [ "$CLEAN" -eq 1 ]; then
        rm -rf "$BUILD_DIR"
    fi
}
trap cleanup EXIT

mkdir -p "$BUILD_DIR"

"$CMAKE" -S "$SCRIPT_DIR" -B "$BUILD_DIR" -G Ninja -DCMAKE_BUILD_TYPE=Debug
"$CMAKE" --build "$BUILD_DIR" --target dns_override_test -- -v

"$BUILD_DIR/dns_override_test"
