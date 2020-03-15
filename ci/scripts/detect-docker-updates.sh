#!/bin/bash

latest_version=$(curl -I -s https://github.com/docker/docker-ce/releases/latest | grep "location:" | awk '{n=split($0, parts, "/"); print substr(parts[n],2);}' | awk '{$1=$1;print}' | tr -d '\r' | tr -d '\n' )

if [[ $latest_version =~ (beta|rc) ]]; then
	echo "Skip pre-release versions"
	exit 0;
fi

latest="https://download.docker.com/linux/static/stable/x86_64/docker-$latest_version.tgz"
current=$( git-repo/ci/images/get-docker-url.sh )

if [[ $current = $latest ]]; then
	echo "Already up-to-date"
	exit 0;
fi


issue_title="Upgrade to Docker $latest_version in CI"

existing_tasks=$( curl -s https://api.github.com/repos/${GITHUB_ORGANIZATION}/${GITHUB_REPO}/pulls\?labels\=type:%20task\&state\=open\&creator\=spring-buildmaster )
existing_upgrade_issues=$( echo "$existing_tasks" | jq -c --arg TITLE "$issue_title" '.[] | select(.title==$TITLE)' )

if [[ ${existing_upgrade_issues} = "" ]]; then
	# Create changes in dedicated branch
	branch="ci-docker-$latest_version"
	git config user.name "Spring Buildmaster" > /dev/null
	git config user.email "buildmaster@springframework.org" > /dev/null
	git checkout -b "$branch" origin/master > /dev/null
	sed -i "s/version=.*/version=\"$latest_version\"/" git-repo/ci/images/get-docker-url.sh
	git add git-repo/ci/images/get-docker-url.sh > /dev/null
	git commit -m "$issue_title" > /dev/null
	git push origin "$branch" > /dev/null

	# Create pull request
	curl \
	-s \
	-u ${GITHUB_USERNAME}:${GITHUB_PASSWORD} \
	-H "Content-type:application/json" \
	-d "{\"head\":\"${branch}\",\"base\":\"master\",\"title\":\"${ISSUE_TITLE}\",\"body\": \"Upgrade to Docker ${latest_version}\",\"labels\":[\"status: waiting-for-triage\",\"type: task\"]}"  \
	-f \
	-X \
	POST "https://api.github.com/repos/${GITHUB_ORGANIZATION}/${GITHUB_REPO}/pulls" > /dev/null || { echo "Failed to create pull request" >&2; exit 1; }
else
	echo "Pull request already exists."
fi
