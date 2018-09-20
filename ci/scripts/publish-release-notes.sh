#!/bin/bash
set -e

source $(dirname $0)/common.sh

latest=$( curl -s "https://api.github.com/repos/${GITHUB_ORGANIZATION}/${GITHUB_REPO}/releases/latest" -u ${GITHUB_USERNAME}:${GITHUB_PASSWORD} )
id=$( echo $latest | jq -r '.id' )

milestone=$( cat version/stageVersion )
milestone_number=$( curl -s "https://api.github.com/repos/${GITHUB_ORGANIZATION}/${GITHUB_REPO}/milestones" -u ${GITHUB_USERNAME}:${GITHUB_PASSWORD} | jq -r --arg MILESTONE "${milestone}" '.[]  | select(.title == $MILESTONE) | .number')

pushd release-notes-repo > /dev/null
run_maven clean install
java -jar -Dreleasenotes.github.organization=${GITHUB_ORGANIZATION} -Dreleasenotes.github.name=${GITHUB_REPO} target/github-release-notes-generator-0.0.1-SNAPSHOT.jar "${milestone_number}" release-notes.md
popd > /dev/null


body=$( sed -E ':a;N;$!ba;s/\r{0,1}\n/\\n/g' release-notes-repo/release-notes.md )

curl \
	-s \
	-u ${GITHUB_USERNAME}:${GITHUB_PASSWORD} \
	-H "Content-type:application/json" \
	-d "{\"body\": \"${body}\"}"  \
	-f \
	-X \
	PATCH "https://api.github.com/repos/${GITHUB_ORGANIZATION}/${GITHUB_REPO}/releases/${id}" > /dev/null || { echo "Failed to publish" >&2; exit 1; }
