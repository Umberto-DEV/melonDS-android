#!/usr/bin/env bash
set -euo pipefail
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_dir="$(cd "$script_dir/../../.." && pwd)"
build_dir="$(mktemp -d "${TMPDIR:-/tmp}/melonds-microphone.XXXXXXXX")"
trap 'rm -rf "$build_dir"' EXIT
source_file="${1:-$repo_dir/app/src/main/cpp/MicInputOboeCallback.cpp}"
"${CXX:-clang++}" -std=c++17 -O1 -g -Wall -Wextra \
    -fsanitize=address,undefined,float-cast-overflow -fno-sanitize-recover=all \
    -fno-omit-frame-pointer \
    -I"$script_dir/stubs" -I"$repo_dir/app/src/main/cpp" -I"$repo_dir/melonDS-android-lib/src" \
    "$script_dir/MicInputGainTest.cpp" "$source_file" -o "$build_dir/MicInputGainTest"
"$build_dir/MicInputGainTest"
