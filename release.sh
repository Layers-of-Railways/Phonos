#!/bin/bash

# Creates a tagged version, and Github Actions `build.yml` will create a Github release.
# To create a Modrinth release, trigger a manual run of `release.yml` in the Actions tab.

if [ $# -ne 1 ]; then
  echo "Usage: $0 \"<release message>\""
  exit 1
fi

raw_version=$(grep -E 'mod_version(\s*)=(\s*)' "./gradle.properties" | cut -d'=' -f2 | tr -d ' ')
mc_version=$(grep -E 'minecraft_version(\s*)=(\s*)' "./gradle.properties" | cut -d'=' -f2 | tr -d ' ')

version="$raw_version+$mc_version"

echo "Creating tag for version $version with message '$1'"

git tag -s -a v"$version" -m "$1"

git push origin v"$version"