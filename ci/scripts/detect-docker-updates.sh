#!/bin/bash

latest_version=$(curl -I -s https://github.com/docker/docker-ce/releases/latest | grep "location:" | awk '{n=split($0, parts, "/"); print substr(parts[n],2);}' | awk '{$1=$1;print}' | tr -d '\r' | tr -d '\n' )
title_prefix="Upgrade CI to Docker"

milestone_number=$( curl -s https://api.github.com/repos/${GITHUB_ORGANIZATION}/${GITHUB_REPO}/milestones\?state\=open | jq -c --arg MILESTONE "$MILESTONE" '.[] | select(.title==$MILESTONE)' | jq -r '.number')

existing_upgrade_issues=$( curl -s https://api.github.com/repos/${GITHUB_ORGANIZATION}/${GITHUB_REPO}/issues\?labels\=type:%20task\&state\=open\&creator\=spring-buildmaster\&milestone\=${milestone_number} | jq -c --arg TITLE_PREFIX "$title_prefix" '.[] | select(.pull_request != null) | select(.title | startswith($TITLE_PREFIX))' )

if [[ ${existing_upgrade_issues} = "" ]]; then
  git clone git-repo git-repo-updated > /dev/null
else
  git clone git-repo-ci-docker git-repo-updated > /dev/null
  echo "Pull request already exists."
  exit 0
fi

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

pushd git-repo-updated > /dev/null
git config user.name "Spring Buildmaster" > /dev/null
git config user.email "buildmaster@springframework.org" > /dev/null
sed -i "s/version=.*/version=\"$latest_version\"/" ci/images/get-docker-url.sh
git add ci/images/get-docker-url.sh > /dev/null
commit_message="$title_prefix $latest_version"
git commit -m "$commit_message" > /dev/null
popd
echo ${commit_message} > commit-details/message