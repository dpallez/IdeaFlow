#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 || ! "$1" =~ ^v[0-9]+\.[0-9]+\.[0-9]+(-(alpha|beta|rc)\.[0-9]+)?$ ]]; then
    echo "Usage: package-release.sh vMAJOR.MINOR.PATCH[-alpha.NUMBER|-beta.NUMBER|-rc.NUMBER]" >&2
    exit 2
fi

release_tag="$1"
source_directory="ideaflow-knime/IDEAFlow.update/target"
source_zip="$(find "$source_directory" -maxdepth 1 -type f -name 'org.ideaflow.update-*.zip' -print -quit)"

if [[ -z "$source_zip" ]]; then
    echo "The Maven build did not produce an update-site ZIP in $source_directory" >&2
    exit 1
fi

mkdir -p dist
release_name="IdeaFlow-${release_tag}-update-site.zip"
cp "$source_zip" "dist/$release_name"
(
    cd dist
    sha256sum "$release_name" > "$release_name.sha256"
)
