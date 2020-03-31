#!/bin/bash
set -e

commit_message=$( cat commit-details/message )

if [[ ${commit_message} != "" ]]; then
	curl \
	  -s \
		-u ${GITHUB_USERNAME}:${GITHUB_PASSWORD} \
		-H "Content-type:application/json" \
		-d "{\"head\":\"${BRANCH}\",\"base\":\"${BASE_BRANCH}\",\"title\":\"${ISSUE_TITLE}\",\"body\":\"\",\"labels\":[\"status: waiting-for-triage\",\"type: task\"]}"  \
		-f \
		-X \
		POST "https://api.github.com/repos/${GITHUB_ORGANIZATION}/${GITHUB_REPO}/pulls" > /dev/null || { echo "Failed to create pull request" >&2; exit 1; }
else
	echo "Already up-to-date"
fi