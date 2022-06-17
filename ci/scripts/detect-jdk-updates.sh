#!/bin/bash

report_error() {
	echo "Script exited with error $1 on line $2"
	exit 1;
}

trap 'report_error $? $LINENO' ERR

case "$JDK_VERSION" in
	java17)
		 BASE_URL="https://api.bell-sw.com/v1/liberica/releases?version-feature=17"
		 ISSUE_TITLE="Upgrade Java 17 version in CI image and .sdkmanrc"
	;;
	java18)
		 BASE_URL="https://api.bell-sw.com/v1/liberica/releases?version-feature=18"
		 ISSUE_TITLE="Upgrade Java 18 version in CI image"
	;;
	*)
		echo $"Unknown java version"
		exit 1;
esac

response=$( curl -s ${BASE_URL}\&arch\=x86\&bitness\=64\&bundle-type\=jdk\&os\=linux\&package-type\=tar.gz\&version-modifier\=latest )
latest=$( jq -r '.[0].downloadUrl' <<< "$response" )
if [[ ${latest} = "null" || ${latest} = "" ]]; then
	echo "Could not parse JDK response: $response"
	exit 1;
fi

current=$( git-repo/ci/images/get-jdk-url.sh ${JDK_VERSION} )

if [[ $current = $latest ]]; then
	echo "Already up-to-date"
	exit 0;
fi

milestone_response=$( curl -s https://api.github.com/repos/${GITHUB_ORGANIZATION}/${GITHUB_REPO}/milestones\?state\=open )
milestone_result=$( jq -r -c --arg MILESTONE "$MILESTONE" '.[] | select(has("title")) | select(.title==$MILESTONE)' <<< "$milestone_response" )
if [[ ${milestone_result} = "null" || ${milestone_result} = "" ]]; then
	echo "Could not parse milestone: $milestone_response"
	exit 1;
fi

milestone_number=$( jq -r '.number' <<< "$milestone_result" )
existing_tasks=$( curl -s https://api.github.com/repos/${GITHUB_ORGANIZATION}/${GITHUB_REPO}/issues\?labels\=type:%20task\&state\=open\&creator\=spring-builds\&milestone\=${milestone_number} )
existing_jdk_issues=$( jq -r -c --arg TITLE "$ISSUE_TITLE" '.[] | select(has("title")) | select(.title==$TITLE)' <<< "$existing_tasks" )

if [[ ${existing_jdk_issues} = "" ]]; then
	curl \
	-s \
	-u ${GITHUB_USERNAME}:${GITHUB_PASSWORD} \
	-H "Content-type:application/json" \
	-d "{\"title\":\"${ISSUE_TITLE}\",\"milestone\":\"${milestone_number}\",\"body\": \"${latest}\",\"labels\":[\"type: task\"]}"  \
	-f \
	-X \
	POST "https://api.github.com/repos/${GITHUB_ORGANIZATION}/${GITHUB_REPO}/issues" > /dev/null || { echo "Failed to create issue" >&2; exit 1; }
else
	echo "Issue already exists."
fi
