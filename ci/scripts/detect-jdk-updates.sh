#!/bin/bash
set -e

case "$JDK_VERSION" in
	java8)
		 BASE_URL="https://api.adoptopenjdk.net/v2/info/releases/openjdk8"
	;;
	java11)
		 BASE_URL="https://api.adoptopenjdk.net/v2/info/releases/openjdk11"
	;;
	*)
		echo $"Unknown java version"
		exit 1;
esac

response=$( curl -s ${BASE_URL}\?openjdk_impl\=hotspot\&os\=linux\&arch\=x64\&release\=latest\&type\=jdk )
latest=$( jq -r '.binaries[0].binary_link' <<< "$response" )

current=$( git-repo/ci/images/get-jdk-url.sh ${JDK_VERSION} )

if [[ $current = $latest ]]; then
	echo "Already up-to-date"
	exit 0;
fi

existing_issues=$( curl -s https://api.github.com/repos/${GITHUB_ORGANIZATION}/${GITHUB_REPO}/issues\?labels\=${JDK_VERSION}-image-upgrade\&state\=open )
parsed_issues=$(jq -r '.' <<< "$existing_issues" )
if [[ ${parsed_issues} = [] ]]; then
	curl \
	-s \
	-u ${GITHUB_USERNAME}:${GITHUB_PASSWORD} \
	-H "Content-type:application/json" \
	-d "{\"title\":\"Upgrade ${JDK_VERSION} version in CI image\",\"body\": \"${latest}\",\"labels\":[\"${JDK_VERSION}-image-upgrade\"]}"  \
	-f \
	-X \
	POST "https://api.github.com/repos/${GITHUB_ORGANIZATION}/${GITHUB_REPO}/issues" > /dev/null || { echo "Failed to create issue" >&2; exit 1; }
else
	echo "Issue already exists."
fi