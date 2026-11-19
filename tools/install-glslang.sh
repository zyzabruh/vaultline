#!/usr/bin/env bash
# Installs the GLSL compiler used by SkyboxShaderCompileTest under .tools/glslang.
#
# The test is skipped when the binary is missing, so this script is what makes the shader
# compile check reproducible on a fresh machine: run it once, then `./gradlew test` compiles
# every skybox stage with glslang and links each vertex/fragment pair.
set -euo pipefail

version="${GLSLANG_VERSION:-16.6.0}"
root="$(cd "$(dirname "$0")/.." && pwd)"
target="$root/.tools/glslang"
work="$(mktemp -d)"

cleanup() { rm -rf "$work"; }
trap cleanup EXIT

mkdir -p "$target"

case "$(uname -s)" in
    MINGW* | MSYS* | CYGWIN*)
        asset="glslang-$version-windows-x86_64-release.zip"
        binary="glslang.exe"
        ;;
    Darwin)
        asset="glslang-$version-macos-release.zip"
        binary="glslang"
        ;;
    *)
        asset="glslang-$version-linux-x86_64-release.zip"
        binary="glslang"
        ;;
esac

echo "Downloading $asset"
curl -fsSL --retry 3 -o "$work/glslang.zip" \
    "https://github.com/KhronosGroup/glslang/releases/download/$version/$asset"

unzip -q -o "$work/glslang.zip" -d "$work/unpacked"

cp "$work/unpacked/bin/$binary" "$target/$binary"
chmod +x "$target/$binary"

echo "Installed $("$target/$binary" --version | head -n 1) at $target/$binary"
