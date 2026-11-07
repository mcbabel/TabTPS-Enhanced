#!/bin/bash
set -u

cd "$(dirname "$0")"
root=$(pwd)
mkdir -p logs work

newest_jar() {
  ls -t $1 2>/dev/null | grep -v -e unshaded -e sources | head -1
}

java_for() {
  local var="JAVA$1"
  if [ -n "${!var:-}" ]; then
    echo "${!var}"
    return
  fi
  for candidate in "$HOME"/.gradle/jdks/*-"$1"-*/bin/java /usr/lib/jvm/java-"$1"-*/bin/java; do
    if [ -x "$candidate" ]; then
      echo "$candidate"
      return
    fi
  done
}

fetch() {
  local source=$1 sha=$2 java=$3
  if [[ $source == buildtools:* ]]; then
    local rev=${source#buildtools:}
    local bt="$root/work/buildtools-$rev"
    mkdir -p "$bt"
    curl -sSL -o "$bt/BuildTools.jar" https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar || return 1
    (cd "$bt" && "$java" -Xmx2G -jar BuildTools.jar --rev "$rev" --compile spigot --output-dir out > buildtools.log 2>&1) || { echo "BuildTools failed, see $bt/buildtools.log"; return 1; }
    mv "$bt"/out/spigot-*.jar server.jar
    rm -rf "$bt"
    echo "-jar server.jar --nogui" > launch.args
    return
  fi
  if [[ $source == forge:* ]]; then
    local forge=${source#forge:}
    curl -sSL -o installer.jar "https://maven.minecraftforge.net/net/minecraftforge/forge/$forge/forge-$forge-installer.jar" || return 1
    "$java" -jar installer.jar --installServer . > install.log 2>&1 || { echo "Forge installer failed, see $(pwd)/install.log"; return 1; }
    rm -f installer.jar installer.jar.log
    echo "@libraries/net/minecraftforge/forge/$forge/unix_args.txt nogui" > launch.args
    return
  fi
  curl -sSL -o server.jar "$source" || return 1
  if [ "$sha" != "-" ]; then
    echo "$sha  server.jar" | sha256sum -c --quiet || { echo "Checksum mismatch for server.jar"; return 1; }
  fi
  echo "-jar server.jar --nogui" > launch.args
}

run() {
  local name=$1 version=$2 source=$3 sha=$4
  local java jar folder
  java=$(java_for "$version")
  if [ -z "$java" ]; then
    printf '%-14s SKIPPED (no Java %s found, set JAVA%s)\n' "$name" "$version" "$version"
    return
  fi
  if [[ $name == forge-* ]]; then
    local mc=${name#forge-}
    jar=$(newest_jar "../forge/$mc/build/libs/tabtps-forge-mc$mc-*.jar")
    folder=mods
  else
    jar=$(newest_jar "../folia/build/libs/tabtps-folia-*.jar")
    folder=plugins
  fi
  if [ -z "$jar" ]; then
    printf '%-14s SKIPPED (no jar built, run ./gradlew build first)\n' "$name"
    return
  fi
  jar=$(realpath "$jar")
  local dir="$root/work/$name"
  rm -rf "$dir"
  mkdir -p "$dir/$folder"
  cd "$dir" || return
  if ! fetch "$source" "$sha" "$java"; then
    printf '%-14s DOWNLOAD FAILED\n' "$name"
    cd "$root" && rm -rf "$dir"
    return
  fi
  cp "$jar" "$folder/"
  echo eula=true > eula.txt
  printf 'online-mode=false\nserver-port=%s\nview-distance=4\nsimulation-distance=4\nlevel-type=minecraft\\:flat\n' "${PORT:-25599}" > server.properties
  : > cmd.txt
  (tail -n +1 -f "$dir/cmd.txt" | "$java" -Dtabtps.testserver="$dir" -Xms1G -Xmx2G $(cat launch.args) > console.log 2>&1) &
  local server=$!
  timeout 300 bash -c 'until grep -qE "Done \(|Failed to start|Encountered an unexpected exception|Crash report saved" console.log; do sleep 1; done'
  for command in "tickinfo" "tabtps reload" "tickinfo" "memory" "tabtps about" "ping" "pingall"; do
    echo "$command" >> cmd.txt
    sleep 2
  done
  echo stop >> cmd.txt
  timeout 120 bash -c "while pgrep -f '[t]abtps.testserver=$dir' > /dev/null; do sleep 1; done"
  pkill -f "[t]abtps.testserver=$dir" 2>/dev/null
  pkill -f "[t]ail -n \+1 -f $dir/cmd.txt" 2>/dev/null
  wait "$server" 2>/dev/null
  cp console.log "$root/logs/$name.log"
  local mode tps exceptions
  if grep -q "Successfully initialized Folia region tick data accessor" console.log; then
    mode="per region"
  elif grep -q "is not a regionized server" console.log; then
    mode="global"
  elif [[ $name == forge-* ]] && grep -q "Server Tick Information" console.log; then
    mode="forge"
  else
    mode="NOT ENABLED"
  fi
  tps=$(grep -m1 -oE "TPS: [0-9,. ()a-z]+" console.log | head -1)
  exceptions=$(grep -c "Exception" console.log)
  printf '%-14s %-11s exceptions=%-3s %s\n' "$name" "$mode" "$exceptions" "$tps"
  cd "$root"
  [ "${KEEP:-}" = 1 ] || rm -rf "$dir"
}

if [ $# -eq 0 ]; then
  echo "Usage: $0 all | <name>...   (names from servers.txt)"
  awk '{print "  " $1}' servers.txt
  exit 1
fi

while read -r name version source sha; do
  [ -z "$name" ] && continue
  if [ "$1" = "all" ] || printf '%s\n' "$@" | grep -qx "$name"; then
    run "$name" "$version" "$source" "$sha"
  fi
done < servers.txt

rmdir work 2>/dev/null
echo "Logs: $root/logs"
