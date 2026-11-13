#!/bin/sh
# Maven Wrapper simplificado: baixa a versão fixa do Maven (definida em
# .mvn/wrapper/maven-wrapper.properties) para ~/.m2/wrapper e executa.
# Assim todos (dev, professor e CI) usam a MESMA versão do Maven.
set -e
BASEDIR=$(cd "$(dirname "$0")" && pwd)
PROPS="$BASEDIR/.mvn/wrapper/maven-wrapper.properties"
URL=$(grep '^distributionUrl=' "$PROPS" | cut -d= -f2-)
NAME=$(basename "$URL" -bin.zip)
DEST="${MAVEN_USER_HOME:-$HOME/.m2}/wrapper/dists/$NAME"
if [ ! -x "$DEST/bin/mvn" ]; then
  echo "[mvnw] Baixando $NAME ..." >&2
  mkdir -p "$DEST.tmp"
  TMPZIP="$DEST.tmp/maven.zip"
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL -o "$TMPZIP" "$URL"
  else
    wget -q -O "$TMPZIP" "$URL"
  fi
  unzip -q -o "$TMPZIP" -d "$DEST.tmp"
  rm -rf "$DEST"
  mv "$DEST.tmp/$NAME" "$DEST"
  rm -rf "$DEST.tmp"
fi
exec "$DEST/bin/mvn" -f "$BASEDIR/pom.xml" "$@"
