#!/bin/bash

latest_version=$(curl -I -s https://github.com/docker/docker-ce/releases/latest | grep -i "location:" | awk '{n=split($0, parts, "/"); print substr(parts[n],2);}' | awk '{$1=$1;print}' | tr -d '\r' | tr -d '\n' )

if [[ $latest_version =~ (beta|rc) ]]; then
	echo "Skip pre-release versions"
	exit 0;
fi

title_prefix="Upgrade CI to Docker"
milestone_number=$( curl -s https://api.github.com/repos/${GITHUB_ORGANIZATION}/${GITHUB_REPO}/milestones\?state\=open | jq -c --arg MILESTONE "$MILESTONE" '.[] | select(.title==$MILESTONE)' | jq -r '.number')
existing_upgrade_issues=$( curl -s https://api.github.com/repos/${GITHUB_ORGANIZATION}/${GITHUB_REPO}/issues\?labels\=type:%20task\&state\=open\&creator\=spring-buildmaster\&milestone\=${milestone_number} | jq -c --arg TITLE_PREFIX "$title_prefix" '.[] | select(.title | startswith($TITLE_PREFIX))' )

latest="https://download.docker.com/linux/static/stable/x86_64/docker-$latest_version.tgz"
current=$( git-repo/ci/images/get-docker-url.sh )

if [[ $current = $latest ]]; then
	echo "Already up-to-date"
	exit 0;
fi

ISSUE_TITLE="$title_prefix $latest_version"

if [[ ${existing_upgrade_issues} = "" ]]; then
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