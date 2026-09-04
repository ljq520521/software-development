#!/bin/sh
set -eu
cd "$(dirname "$0")"
set -- -s "$(pwd)/.mvn/settings.xml" "$@"
if command -v mvn >/dev/null 2>&1; then exec mvn "$@"; fi
IDEA_MAVEN='/Applications/IntelliJ IDEA.app/Contents/plugins/maven-plugin/lib/maven3/bin/mvn'
if [ -x "$IDEA_MAVEN" ]; then exec "$IDEA_MAVEN" "$@"; fi
MAVEN_VERSION=3.9.11
MAVEN_DIR="$(pwd)/.mvn/apache-maven-$MAVEN_VERSION"
if [ ! -x "$MAVEN_DIR/bin/mvn" ]; then
  mkdir -p .mvn
  BASE="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MAVEN_VERSION/apache-maven-$MAVEN_VERSION-bin.tar.gz"
  curl -fL "$BASE" -o .mvn/maven.tar.gz
  curl -fL "$BASE.sha512" -o .mvn/maven.sha512
  EXPECTED=$(tr -d '\r\n' < .mvn/maven.sha512)
  ACTUAL=$(shasum -a 512 .mvn/maven.tar.gz | cut -d ' ' -f 1)
  [ "$EXPECTED" = "$ACTUAL" ] || { echo 'Maven checksum mismatch'; exit 1; }
  tar -xzf .mvn/maven.tar.gz -C .mvn
fi
exec "$MAVEN_DIR/bin/mvn" "$@"
