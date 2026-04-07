#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/.." && pwd)"
cw3_dir="$repo_root/CW3"
output_dir="$cw3_dir/target/javadocs/CW3SECodeReviewImplementationGroup22-javadocs"
javadoc_classpath_file="$cw3_dir/target/task4-javadoc.classpath"

rm -rf "$output_dir"
mkdir -p "$output_dir"

javadoc_sources_file="$output_dir/.sources.txt"

cat > "$javadoc_sources_file" <<EOF
$cw3_dir/src/main/java/uk/ac/ed/inf/eventsapp/facultypreregistration/FacultyMember.java
$cw3_dir/src/main/java/uk/ac/ed/inf/eventsapp/facultypreregistration/RegistrationUtility.java
$cw3_dir/src/main/java/uk/ac/ed/inf/eventsapp/model/User.java
$cw3_dir/src/main/java/uk/ac/ed/inf/eventsapp/util/PasswordUtils.java
EOF

javadoc \
  -quiet \
  -d "$output_dir" \
  -classpath "$(cat "$javadoc_classpath_file")" \
  -sourcepath "$cw3_dir/src/main/java" \
  @"$javadoc_sources_file"
