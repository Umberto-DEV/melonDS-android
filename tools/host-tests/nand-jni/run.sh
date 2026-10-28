#!/bin/sh
set -eu
export UBSAN_OPTIONS="${UBSAN_OPTIONS:-halt_on_error=1:print_stacktrace=1}"
script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
repo_dir=$(CDPATH= cd -- "$script_dir/../../.." && pwd)
build_dir=$(mktemp -d "${TMPDIR:-/tmp}/melonds-nand-jni.XXXXXX")
trap 'rm -rf "$build_dir"' EXIT HUP INT TERM
# Compile the unchanged production translation unit beside minimal core/JNI boundary stubs.
# The copy makes quoted includes resolve to stubs without altering production headers or source.
production_source=${1:-"$repo_dir/app/src/main/cpp/MelonDSNandJNI.cpp"}
cp "$production_source" "$build_dir/MelonDSNandJNI.cpp"
${CXX:-c++} -std=c++17 -Wall -Wextra -Wno-unused-parameter -Wno-deprecated-declarations \
    -fsanitize=address,undefined -fno-omit-frame-pointer \
    -I"$build_dir" -I"$script_dir/stubs" "$script_dir/NandJniTest.cpp" -o "$build_dir/nand-jni-test"
"$build_dir/nand-jni-test"
