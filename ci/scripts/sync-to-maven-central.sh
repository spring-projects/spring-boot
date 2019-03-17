#!/bin/bash
set -e

buildName=$( cat artifactory-repo/build-info.json | jq -r '.buildInfo.name' )
buildNumber=$( cat artifactory-repo/build-info.json | jq -r '.buildInfo.number' )
groupId=$( cat artifactory-repo/build-info.json | jq -r '.buildInfo.modules[0].id' | sed 's/\(.*\):.*:.*/\1/' )
version=$( cat artifactory-repo/build-info.json | jq -r '.buildInfo.modules[0].id' | sed 's/.*:.*:\(.*\)/\1/' )

echo "Syncing ${buildName}/${buildNumber} to Maven Central"
	curl \
			-s \
			--connect-timeout 240 \
			--max-time 2700 \
			-u ${BINTRAY_USERNAME}:${BINTRAY_API_KEY} \
			-H "Content-Type: application/json" -d "{\"username\": \"${SONATYPE_USER_TOKEN}\", \"password\": \"${SONATYPE_PASSWORD_TOKEN}\"}" \
			-f \
			-X \
			POST "https://api.bintray.com/maven_central_sync/${BINTRAY_SUBJECT}/${BINTRAY_REPO}/${groupId}/versions/${version}" > /dev/null || { echo "Failed to sync" >&2; exit 1; }
echo "Sync complete"
