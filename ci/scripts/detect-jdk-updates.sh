#!/bin/bash

case "$JDK_VERSION" in
	java8)
		 BASE_URL="https://api.adoptopenjdk.net/v3/assets/feature_releases/8"
		 ISSUE_TITLE="Upgrade Java 8 version in CI image"
	;;
	java11)
		 BASE_URL="https://api.adoptopenjdk.net/v3/assets/feature_releases/11"
		 ISSUE_TITLE="Upgrade Java 11 version in CI image"
	;;
	java14)
		 BASE_URL="https://api.adoptopenjdk.net/v3/assets/feature_releases/14"
		 ISSUE_TITLE="Upgrade Java 14 version in CI image"
	;;
	*)
		echo $"Unknown java version"
		exit 1;
esac

response=$( curl -s ${BASE_URL}\/ga\?architecture\=x64\&heap_size\=normal\&image_type\=jdk\&jvm_impl\=hotspot\&os\=linux\&sort_order\=DESC\&vendor\=adoptopenjdk )
latest=$( jq -r '.[0].binaries[0].package.link' <<< "$response" )

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
