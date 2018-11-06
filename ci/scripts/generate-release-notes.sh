#!/bin/bash
set -e

source $(dirname $0)/common.sh

version=$( cat version/version )
milestone=$( echo $version )
if [[ $RELEASE_TYPE = "RELEASE" ]]; then
	milestone=${version%.RELEASE}
fi
milestone_number=$( curl -s "https://api.github.com/repos/${GITHUB_ORGANIZATION}/${GITHUB_REPO}/milestones" -u ${GITHUB_USERNAME}:${GITHUB_PASSWORD} | jq -r --arg MILESTONE "${milestone}" '.[]  | select(.title == $MILESTONE) | .number')

pushd release-notes-repo > /dev/null
run_maven clean install
java -jar -Dreleasenotes.github.organization=${GITHUB_ORGANIZATION} -Dreleasenotes.github.name=${GITHUB_REPO} target/github-release-notes-generator-0.0.1-SNAPSHOT.jar "${milestone_number}" release-notes.md
popd > /dev/null

cat release-notes-repo/release-notes.md > generated-release-notes/body
echo v${version} > generated-release-notes/version
