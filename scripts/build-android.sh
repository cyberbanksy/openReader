#!/bin/sh
set -eu

project_root=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
source_root="$project_root/native/android"
work_root=$(mktemp -d "${TMPDIR:-/tmp}/openreader-android.XXXXXX")

cleanup() {
  chmod -R u+w "$work_root" 2>/dev/null || true
  find "$work_root" -depth -delete 2>/dev/null || true
}
trap cleanup EXIT INT TERM

rsync -a \
  --exclude '.gradle' \
  --exclude 'build' \
  --exclude 'app/build' \
  --exclude 'dist' \
  "$source_root/" "$work_root/"

cd "$work_root"
if [ "$#" -eq 0 ]; then
  set -- testDebugUnitTest lintDebug assembleDebug
fi
./gradlew --no-daemon "$@"

apk="$work_root/app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$apk" ]; then
  mkdir -p "$source_root/dist"
  cp "$apk" "$source_root/dist/openreader-android-debug.apk"
  shasum -a 256 "$source_root/dist/openreader-android-debug.apk" > \
    "$source_root/dist/openreader-android-debug.apk.sha256"
  echo "APK: $source_root/dist/openreader-android-debug.apk"
fi
