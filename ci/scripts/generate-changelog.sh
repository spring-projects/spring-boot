#!/bin/bash
set -e

version=$( cat version/version )

milestone=${version}
if [[ $RELEASE_TYPE = "RELEASE" ]]; then
	milestone=${version%.RELEASE}
fi

java -jar /github-changelog-generator.jar \
  --changelog.repository=spring-projects/spring-boot  \
  ${milestone} generated-changelog/changelog.md

echo ${version} > generated-changelog/version
echo v${version} > generated-changelog/tag
