#!/bin/bash

case "$JDK_VERSION" in
	java8)
		 BASE_URL="https://api.adoptopenjdk.net/v2/info/releases/openjdk8"
		 ISSUE_TITLE="Upgrade Java 8 version in CI image"
	;;
	java11)
		 BASE_URL="https://api.adoptopenjdk.net/v2/info/releases/openjdk11"
		 ISSUE_TITLE="Upgrade Java 11 version in CI image"
	;;
	java12)
		 BASE_URL="https://api.adoptopenjdk.net/v2/info/releases/openjdk12"
		 ISSUE_TITLE="Upgrade Java 12 version in CI image"
	;;
	java13)
		 BASE_URL="https://api.adoptopenjdk.net/v2/info/releases/openjdk13"
		 ISSUE_TITLE="Upgrade Java 13 version in CI image"
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

existing_tasks=$( curl -s https://api.github.com/repos/${GITHUB_ORGANIZATION}/${GITHUB_REPO}/issues\?labels\=type:%20task\&state\=open\&creator\=spring-buildmaster )
existing_jdk_issues=$( echo "$existing_tasks" | jq -c --arg TITLE "$ISSUE_TITLE" '.[] | select(.title==$TITLE)' )

if [[ ${existing_jdk_issues} = "" ]]; then
	curl \
	-s \
	-u ${GITHUB_USERNAME}:${GITHUB_PASSWORD} \
	-H "Content-type:application/json" \
	-d "{\"title\":\"${ISSUE_TITLE}\",\"body\": \"${latest}\",\"labels\":[\"status: waiting-for-triage\",\"type: task\"]}"  \
	-f \
	-X \
	POST "https://api.github.com/repos/${GITHUB_ORGANIZATION}/${GITHUB_REPO}/issues" > /dev/null || { echo "Failed to create issue" >&2; exit 1; }
else
	echo "Issue already exists."
fi
