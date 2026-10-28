#!/usr/bin/env bash
set -euo pipefail
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_dir="$(cd "$script_dir/../../../.." && pwd)"
build_dir="$(mktemp -d "${TMPDIR:-/tmp}/melonds-mic-lifecycle.XXXXXXXX")"
trap 'rm -rf "$build_dir"' EXIT
# Copy only to resolve quoted frontend includes to the explicit boundary adapters.
# The production read/capture/lifecycle implementations remain unchanged.
cp "${1:-$repo_dir/app/src/main/cpp/MelonDSAudio.cpp}" "$build_dir/MelonDSAudio.cpp"
"${CXX:-clang++}" -std=c++17 -O1 -g -Wall -Wextra -fno-omit-frame-pointer \
    -fsanitize=address,undefined -fno-sanitize-recover=all \
    -I"$script_dir/stubs" -I"$repo_dir/app/src/main/cpp" \
    -I"$repo_dir/melonDS-android-lib/src" -I"$repo_dir/melonDS-android-lib/src/frontend" \
    "$script_dir/MicSourceSwitchTest.cpp" "$build_dir/MelonDSAudio.cpp" \
    "$repo_dir/app/src/main/cpp/MicInputOboeCallback.cpp" \
    "$repo_dir/app/src/main/cpp/AudioOutputPolicy.cpp" -o "$build_dir/MicSourceSwitchTest"
if [ "$#" -gt 1 ]; then "$build_dir/MicSourceSwitchTest" "$2"; else "$build_dir/MicSourceSwitchTest"; fi
