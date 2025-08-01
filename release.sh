#!/bin/bash

raw_version=$(grep -E 'mod_version(\s*)=(\s*)' "./gradle.properties" | cut -d'=' -f2 | tr -d ' ')
mc_version=$(grep -E 'minecraft_version(\s*)=(\s*)' "./gradle.properties" | cut -d'=' -f2 | tr -d ' ')

version="$raw_version+$mc_version"

git tag -s -a v"$version" -m "$1"

git push origin main v"$version"