#!/bin/bash
set -e

version=$( cat version/version )

milestone=${version}
if [[ $RELEASE_TYPE = "RELEASE" ]]; then
	milestone=${version%.RELEASE}
fi

java -jar /github-release-notes-generator.jar \
  --releasenotes.github.username=${GITHUB_USERNAME} \
  --releasenotes.github.password=${GITHUB_TOKEN} \
  --releasenotes.github.organization=spring-projects \
  --releasenotes.github.repository=spring-boot  \
  ${milestone} generated-release-notes/release-notes.md

echo ${version} > generated-release-notes/version
echo v${version} > generated-release-notes/tag
