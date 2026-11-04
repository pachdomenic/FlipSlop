#!/bin/bash
#
# Builds the release APK with Gradle and drops it where lineage/Android.bp
# expects it. Run from anywhere; needs ANDROID_HOME (or local.properties).
#
#   ./lineage/update-prebuilt.sh
#
# Sign with your own key (keystore.properties in the repo root) if you want to
# update the system app later with `adb install`; the debug key works for a
# one-off ROM build.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT}"

./gradlew :app:assembleRelease
mkdir -p lineage/prebuilt
cp app/build/outputs/apk/release/app-release.apk lineage/prebuilt/FlipSlop.apk
echo "Updated lineage/prebuilt/FlipSlop.apk"
