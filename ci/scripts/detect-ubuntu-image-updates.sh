#!/bin/bash

ISSUE_TITLE="Upgrade Ubuntu version in CI images"

ubuntu="bionic"
latest=$( curl -s "https://hub.docker.com/v2/repositories/library/ubuntu/tags/?page_size=1&page=1&name=$ubuntu" | jq -c -r '.results[0].name' | awk '{split($0, parts, "-"); print parts[2]}' )
current=$( grep "ubuntu:$ubuntu" git-repo/ci/images/spring-boot-ci-image/Dockerfile | awk '{split($0, parts, "-"); print parts[2]}' )

if [[ $current = $latest ]]; then
	echo "Already up-to-date"
	exit 0;
fi

milestone_number=$( curl -s https://api.github.com/repos/${GITHUB_ORGANIZATION}/${GITHUB_REPO}/milestones\?state\=open | jq -c --arg MILESTONE "$MILESTONE" '.[] | select(.title==$MILESTONE)' | jq -r '.number')
existing_tasks=$( curl -s https://api.github.com/repos/${GITHUB_ORGANIZATION}/${GITHUB_REPO}/issues\?labels\=type:%20task\&state\=open\&creator\=spring-buildmaster\&milestone\=${milestone_number} )
existing_upgrade_issues=$( echo "$existing_tasks" | jq -c --arg TITLE "$ISSUE_TITLE" '.[] | select(.title==$TITLE)' )

if [[ ${existing_upgrade_issues} = "" ]]; then
	curl \
	-s \
	-u ${GITHUB_USERNAME}:${GITHUB_PASSWORD} \
	-H "Content-type:application/json" \
	-d "{\"title\":\"${ISSUE_TITLE}\",\"milestone\":\"${milestone_number}\",\"body\": \"Upgrade to ubuntu:${ubuntu}-${latest}\",\"labels\":[\"status: waiting-for-triage\",\"type: task\"]}"  \
	-f \
	-X \
	POST "https://api.github.com/repos/${GITHUB_ORGANIZATION}/${GITHUB_REPO}/issues" > /dev/null || { echo "Failed to create issue" >&2; exit 1; }
else
	echo "Issue already exists."
fi
