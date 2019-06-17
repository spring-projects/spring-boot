#!/bin/bash
set -e

buildName=$( cat artifactory-repo/build-info.json | jq -r '.buildInfo.name' )
buildNumber=$( cat artifactory-repo/build-info.json | jq -r '.buildInfo.number' )
groupId=$( cat artifactory-repo/build-info.json | jq -r '.buildInfo.modules[0].id' | sed 's/\(.*\):.*:.*/\1/' )
version=$( cat artifactory-repo/build-info.json | jq -r '.buildInfo.modules[0].id' | sed 's/.*:.*:\(.*\)/\1/' )

echo "Syncing ${buildName}/${buildNumber} to Maven Central"

	publishStatus=$(curl \
			-s \
			-o /dev/null \
			-I \
			-w "%{http_code}"
			"https://oss.sonatype.org/service/local/repositories/releases/content/org/springframework/boot/spring-boot/${version}/spring-boot-${version}.jar.sha1")

	if [ ${publishStatus} == "200" ]; then
		echo "Already published to Sonatype"
	else
		echo "Calling Bintray to sync to Sonatype"
		curl \
				-s \
				--connect-timeout 240 \
				--max-time 2700 \
				-u ${BINTRAY_USERNAME}:${BINTRAY_API_KEY} \
				-H "Content-Type: application/json" -d "{\"username\": \"${SONATYPE_USER_TOKEN}\", \"password\": \"${SONATYPE_PASSWORD_TOKEN}\"}" \
				-f \
				-X \
				POST "https://api.bintray.com/maven_central_sync/${BINTRAY_SUBJECT}/${BINTRAY_REPO}/${groupId}/versions/${version}" > /dev/null || { echo "Failed to sync" >&2; exit 1; }
	fi

echo "Sync complete"
echo $version > version/version
