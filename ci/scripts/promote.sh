#!/bin/bash
set -e

source $(dirname $0)/common.sh

buildName=$( cat artifactory-repo/build-info.json | jq -r '.buildInfo.name' )
buildNumber=$( cat artifactory-repo/build-info.json | jq -r '.buildInfo.number' )
groupId=$( cat artifactory-repo/build-info.json | jq -r '.buildInfo.modules[0].id' | sed 's/\(.*\):.*:.*/\1/' )
version=$( cat artifactory-repo/build-info.json | jq -r '.buildInfo.modules[0].id' | sed 's/.*:.*:\(.*\)/\1/' )


if [[ $RELEASE_TYPE = "M" ]]; then
	targetRepo="libs-milestone-local"
elif [[ $RELEASE_TYPE = "RC" ]]; then
	targetRepo="libs-milestone-local"
elif [[ $RELEASE_TYPE = "RELEASE" ]]; then
	targetRepo="libs-release-local"
else
	echo "Unknown release type $RELEASE_TYPE" >&2; exit 1;
fi

echo "Promoting ${buildName}/${buildNumber} to ${targetRepo}"

curl \
	-s \
	--connect-timeout 240 \
	--max-time 900 \
	-u ${ARTIFACTORY_USERNAME}:${ARTIFACTORY_PASSWORD} \
	-H "Content-type:application/json" \
	-d "{\"status\": \"staged\", \"sourceRepo\": \"libs-staging-local\", \"targetRepo\": \"${targetRepo}\"}"  \
	-f \
	-X \
	POST "${ARTIFACTORY_SERVER}/api/build/promote/${buildName}/${buildNumber}" > /dev/null || { echo "Failed to promote" >&2; exit 1; }

if [[ $RELEASE_TYPE = "RELEASE" ]]; then
	curl \
		-s \
		--connect-timeout 240 \
		--max-time 900 \
		-u ${ARTIFACTORY_USERNAME}:${ARTIFACTORY_PASSWORD} \
		-H "Content-type:application/json" \
		-u ${ARTIFACTORY_USERNAME}:${ARTIFACTORY_PASSWORD} \
		-d "{\"sourceRepos\": [\"libs-release-local\"], \"targetRepo\" : \"spring-distributions\"}" \
		-f \
		-X \
		POST "${ARTIFACTORY_SERVER}/api/build/distribute/${buildName}/${buildNumber}" > /dev/null || { echo "Failed to publish" >&2; exit 1; }

	curl \
		-s \
		--connect-timeout 240 \
		--max-time 900 \
		-u ${BINTRAY_USERNAME}:${BINTRAY_PASSWORD} \
		-H "Content-Type: application/json" -d "{\"username\": \"${SONATYPE_USERNAME}\", \"password\": \"${SONATYPE_PASSWORD}\"}" \
		-f \
		-X \
		POST "https://api.bintray.com/maven_central_sync/${BINTRAY_SUBJECT}/${BINTRAY_REPO}/${groupId}/versions/${version}" > /dev/null || { echo "Failed to sync" >&2; exit 1; }
fi


echo "Promotion complete"
