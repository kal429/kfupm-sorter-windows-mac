#!/usr/bin/env bash
# Builds KFUPM Sorter Desktop with nothing but a JDK (21 or newer).
#
#   ./build.sh          compile, run the self-test, make build/kfupm-sorter-desktop.jar
#   ./build.sh run      ... and open the window
#   ./build.sh package  ... and make a native app with Java bundled (jpackage):
#                       macOS  -> build/installer/*.dmg
#                       Windows (Git Bash) -> build/image/KFUPM Sorter Desktop/ (then run Inno Setup on packaging/windows.iss)
#                       Linux  -> build/image/KFUPM Sorter Desktop/
set -euo pipefail
cd "$(dirname "$0")"

VERSION=1.1.0
NAME="KFUPM Sorter Desktop"
MODULES=java.base,java.desktop,java.net.http
SEP=":"
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=";" ;; esac

rm -rf build
mkdir -p build/classes build/test build/jar

echo "== compile"
javac -encoding UTF-8 -Xlint:all,-serial -d build/classes $(find src/main/java -name '*.java')
cp -r src/main/resources/* build/classes/

echo "== self-test"
javac -encoding UTF-8 -cp build/classes -d build/test $(find src/test/java -name '*.java')
TESTDATA="$(mktemp -d)"
java -Djava.awt.headless=true -Dkfupmsorter.data="$TESTDATA" -cp "build/classes${SEP}build/test" io.github.kal429.kfupmsorter.SelfTest
rm -rf "$TESTDATA"

echo "== jar"
jar --create --file build/jar/kfupm-sorter-desktop.jar --main-class io.github.kal429.kfupmsorter.Main -C build/classes .

case "${1:-}" in
  run)
    java -jar build/jar/kfupm-sorter-desktop.jar
    ;;
  package)
    COMMON=(--name "$NAME" --input build/jar --main-jar kfupm-sorter-desktop.jar --app-version "$VERSION"
            --vendor "KFUPM Sorter (student project)" --description "Keeps your Downloads folder sorted by KFUPM course"
            --add-modules "$MODULES" --jlink-options "--strip-debug --no-header-files --no-man-pages --compress=zip-6")
    case "$(uname -s)" in
      Darwin)
        jpackage "${COMMON[@]}" --type dmg --icon packaging/app.icns \
                 --mac-package-identifier io.github.kal429.kfupmsorter --mac-package-name "KFUPM Sorter" \
                 --dest build/installer
        ;;
      MINGW*|MSYS*|CYGWIN*)
        jpackage "${COMMON[@]}" --type app-image --icon packaging/app.ico --dest build/image
        ;;
      *)
        jpackage "${COMMON[@]}" --type app-image --icon packaging/app.png --dest build/image
        ;;
    esac
    ;;
esac
echo "== done"
