#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/.." && pwd)"
cw3_dir="$repo_root/CW3"
output_dir="$cw3_dir/target/javadocs/CW3SEImplementationGroup22-javadocs"
javadoc_classpath_file="$cw3_dir/target/se-implementation-javadoc.classpath"

rm -rf "$output_dir"
mkdir -p "$output_dir"

javadoc_sources_file="$output_dir/.sources.txt"

find "$cw3_dir/src/main/java" -name "*.java" \
  ! -path "*/external/*" \
  ! -path "*/uk/ac/ed/inf/eventsapp/facultypreregistration/*" | sort > "$javadoc_sources_file"

javadoc \
  -quiet \
  -d "$output_dir" \
  -classpath "$(cat "$javadoc_classpath_file")" \
  -sourcepath "$cw3_dir/src/main/java" \
  @"$javadoc_sources_file"
