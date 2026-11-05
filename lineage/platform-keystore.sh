#!/usr/bin/env bash
#
# Turns the ROM's platform key into a keystore Gradle can sign with, and writes
# platform.properties next to settings.gradle.kts. With it every Gradle build
# (debug and release) is signed like the FlipSlop the ROM ships
# (certificate: "platform" in Android.bp), so `adb install -r` keeps updating
# the system app and it keeps ACCESS_SHORTCUTS.
#
# Usage: lineage/platform-keystore.sh [KEY_DIR]
#   KEY_DIR holds platform.pk8 and platform.x509.pem: ~/.android-certs for
#   release-keys builds (the default), build/make/target/product/security in
#   the source tree for test-keys builds.
#
# The keystore and platform.properties are git-ignored. Keep them private:
# anything signed with this key is trusted as part of the system.
#
set -euo pipefail

KEY_DIR="${1:-$HOME/.android-certs}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PK8="$KEY_DIR/platform.pk8"
CERT="$KEY_DIR/platform.x509.pem"
STORE="$ROOT/platform.keystore"
PASS="${PLATFORM_STORE_PASS:-flipslop-platform}"
ALIAS=platform

for f in "$PK8" "$CERT"; do
    [[ -f "$f" ]] || { echo "Missing $f" >&2; exit 1; }
done

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

openssl pkcs8 -inform DER -nocrypt -in "$PK8" -out "$TMP/platform.key"
openssl pkcs12 -export -in "$CERT" -inkey "$TMP/platform.key" \
    -name "$ALIAS" -passout "pass:$PASS" -out "$TMP/platform.p12"
rm -f "$STORE"
keytool -importkeystore -noprompt \
    -srckeystore "$TMP/platform.p12" -srcstoretype PKCS12 -srcstorepass "$PASS" \
    -destkeystore "$STORE" -deststoretype PKCS12 -deststorepass "$PASS" -destkeypass "$PASS" \
    -alias "$ALIAS"

cat > "$ROOT/platform.properties" <<EOF
storeFile=platform.keystore
storePassword=$PASS
keyAlias=$ALIAS
keyPassword=$PASS
EOF

echo "Wrote $STORE and $ROOT/platform.properties"
keytool -list -v -keystore "$STORE" -storepass "$PASS" | grep -m1 "SHA256:"
