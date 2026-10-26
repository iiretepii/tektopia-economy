#!/usr/bin/env bash
# Builds the mod jar without ForgeGradle, for machines that can't reach Forge's or Mojang's
# servers. Needs a JDK 11+ (it targets Java 8), git, curl, and libs/tektopia-<version>.jar.
#
#   tools/offline-build/build.sh [corpus-dir]
#
# corpus-dir (optional): a folder of other Forge 1.12.2 mod jars. Every Minecraft, Forge and
# library reference the mod makes is then checked against references in those jars.
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
WORK="$ROOT/build/offline"
CORPUS="${1:-}"

prop() { grep "^$1=" "$ROOT/gradle.properties" | cut -d= -f2; }
VERSION="$(prop mod_version)"
MC="$(prop mc_version)"
TEKTOPIA="$ROOT/libs/tektopia-$(prop tektopia_version).jar"
[ -f "$TEKTOPIA" ] || { echo "Missing $TEKTOPIA (see README.md)"; exit 1; }

mkdir -p "$WORK/deps"
fetch() { # maven-central-path
    local out="$WORK/deps/$(basename "$1")"
    [ -f "$out" ] && return
    for i in 1 2 3 4 5; do
        curl -sSfL -o "$out" "https://repo1.maven.org/maven2/$1" && return
        sleep $((i * 3))
    done
    echo "Couldn't download $1"; exit 1
}
fetch org/ow2/asm/asm/9.7/asm-9.7.jar
fetch org/ow2/asm/asm-commons/9.7/asm-commons-9.7.jar
fetch com/google/guava/guava/21.0/guava-21.0.jar
fetch org/apache/logging/log4j/log4j-api/2.8.1/log4j-api-2.8.1.jar
fetch com/google/code/findbugs/jsr305/3.0.1/jsr305-3.0.1.jar
D="$WORK/deps"
ASM="$D/asm-9.7.jar:$D/asm-commons-9.7.jar"
LIBS="$D/guava-21.0.jar:$D/log4j-api-2.8.1.jar:$D/jsr305-3.0.1.jar"

# MCP 1.12 SRG names and stable_39 names, from the MCP mappings archive on GitHub.
MAP="$WORK/mappings"
if [ ! -f "$MAP/joined.srg" ]; then
    rm -rf "$WORK/mcp-archive"
    git clone -q --depth 1 --filter=blob:none --no-checkout https://github.com/ModCoderPack/MCPMappingsArchive "$WORK/mcp-archive"
    git -C "$WORK/mcp-archive" sparse-checkout set --no-cone /mcp/1.12/mcp-1.12-srg.zip* /mcp_stable/39-1.12/mcp_stable-39-1.12.zip*
    git -C "$WORK/mcp-archive" checkout -q
    mkdir -p "$MAP"
    for z in mcp/1.12/mcp-1.12-srg.zip mcp_stable/39-1.12/mcp_stable-39-1.12.zip; do
        f="$WORK/mcp-archive/$z"
        [ "$(sha1sum "$f" | cut -c1-40)" = "$(cut -c1-40 "$f.sha1")" ] || { echo "Checksum mismatch: $z"; exit 1; }
        unzip -o -q "$f" -d "$MAP"
    done
fi

# TekTopia's entities implement CraftStudio's IAnimated; CraftStudio ships inside the TekTopia jar.
rm -rf "$WORK/tektopia-libs" && mkdir -p "$WORK/tektopia-libs"
unzip -o -q -j "$TEKTOPIA" 'META-INF/libraries/*.jar' -d "$WORK/tektopia-libs"
CRAFTSTUDIO="$(ls "$WORK/tektopia-libs"/*.jar | head -1)"

rm -rf "$WORK/stubs" "$WORK/classes" "$WORK/srg" "$WORK/tools" "$WORK/jar"
mkdir -p "$WORK/stubs" "$WORK/classes" "$WORK/srg" "$WORK/tools" "$WORK/jar"
quiet() { grep -v "JAVA_TOOL_OPTIONS\|SideOnly\|Side.CLIENT\|Side not found\|^[0-9]* warnings\|options\]" || true; }

echo "Compiling..."
javac --release 8 -nowarn -cp "$LIBS" -d "$WORK/stubs" $(find "$HERE/stubs" -name '*.java') 2>&1 | quiet
javac --release 8 -cp "$WORK/stubs:$LIBS:$TEKTOPIA:$CRAFTSTUDIO" -d "$WORK/classes" $(find "$ROOT/src/main/java" -name '*.java') 2>&1 | quiet
[ -f "$WORK/classes/tekeco/TekEconomy.class" ] || { echo "Compile failed"; exit 1; }
javac -nowarn -cp "$ASM" -d "$WORK/tools" "$HERE"/src/*.java 2>&1 | quiet

echo "Converting to SRG names..."
java -cp "$WORK/tools:$ASM" Reobf "$MAP" "$WORK/classes" "$WORK/srg" \
    "$WORK/classes" "$WORK/stubs" "$TEKTOPIA" "$CRAFTSTUDIO" ${LIBS//:/ } 2>&1 | quiet | tee "$WORK/reobf.log"
# Unmapped Minecraft-owned names are only allowed for Forge-added members, members inherited
# from Java classes, TekTopia's own methods and this mod's own members.
UNEXPECTED="$(grep '^UNMAPPED' "$WORK/reobf.log" \
    | grep -v ' tekeco/' \
    | grep -v ' net/tangotek/' \
    | grep -v -E ' net/minecraft/entity/Entity (getCapability|hasCapability) ' \
    | grep -v -E ' net/minecraft/world/World getPerWorldStorage ' \
    | grep -v -E ' net/minecraft/item/Item getRegistryName ' \
    | grep -v -E ' net/minecraft/village/MerchantRecipeList iterator ' || true)"
[ -z "$UNEXPECTED" ] || { echo "Unexpected unmapped references:"; echo "$UNEXPECTED"; exit 1; }

if [ -n "$CORPUS" ]; then
    echo "Checking references against $CORPUS..."
    java -cp "$WORK/tools:$ASM" RefCheck "$WORK/srg" net/minecraft,com/google,org/apache,javax/ "$CORPUS"/*.jar 2>&1 | quiet > "$WORK/refcheck.log"
    tail -1 "$WORK/refcheck.log"
    # A vanilla Minecraft member no sampled mod uses still counts if the official mappings
    # declare it with the same signature. Forge and library references must appear in a mod.
    STILL_MISSING=""
    while read -r _ kind owner name desc; do
        case "$owner" in net/minecraftforge/*) ;; net/minecraft/*)
            if [ "$kind" = M ] && grep -qF " $owner/$name $desc" "$MAP/joined.srg"; then echo "In mappings: $owner $name $desc"; continue; fi
            if [ "$kind" = F ] && awk -v f="$owner/$name" '{ sub(/\r$/, "") } $1 == "FD:" && $3 == f { found = 1 } END { exit !found }' "$MAP/joined.srg"; then echo "In mappings: $owner $name"; continue; fi
        esac
        STILL_MISSING="$STILL_MISSING$kind $owner $name $desc"$'\n'
    done < <(grep '^MISSING' "$WORK/refcheck.log" | sort -u)
    if [ -n "$STILL_MISSING" ]; then echo "Missing references:"; printf '%s' "$STILL_MISSING"; exit 1; fi
fi

echo "Packaging..."
cp -r "$WORK/srg/." "$WORK/jar/"
cp -r "$ROOT/src/main/resources/." "$WORK/jar/"
sed -i "s/\${version}/$VERSION/; s/\${mcversion}/$MC/" "$WORK/jar/mcmod.info"
printf 'Manifest-Version: 1.0\nImplementation-Title: TekTopia Economy\nImplementation-Version: %s\n' "$VERSION" > "$WORK/MANIFEST.MF"
mkdir -p "$ROOT/build/libs"
OUT="$ROOT/build/libs/tektopia-economy-$MC-$VERSION.jar"
rm -f "$OUT"
(cd "$WORK/jar" && jar cfm "$OUT" "$WORK/MANIFEST.MF" . 2>&1 | quiet)
echo "Built $OUT"
