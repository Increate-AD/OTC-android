#!/usr/bin/env bash
set -euo pipefail
GRADLE_VERSION="8.9"
GRADLE_HOME_DIR="$HOME/.gradle/otc-gradle-$GRADLE_VERSION"
if [ ! -x "$GRADLE_HOME_DIR/bin/gradle" ]; then
  mkdir -p "$HOME/.gradle"
  ARCHIVE="$HOME/.gradle/otc-gradle-$GRADLE_VERSION-bin.zip"
  curl -fsSL "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$ARCHIVE"
  unzip -q -o "$ARCHIVE" -d "$HOME/.gradle"
  mv "$HOME/.gradle/gradle-$GRADLE_VERSION" "$GRADLE_HOME_DIR"
fi
exec "$GRADLE_HOME_DIR/bin/gradle" "$@"
