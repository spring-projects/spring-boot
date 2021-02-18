#!/bin/bash
set -e

CONFIG_DIR=git-repo/ci/config
version=$( cat artifactory-repo/build-info.json | jq -r '.buildInfo.modules[0].id' | sed 's/.*:.*:\(.*\)/\1/' )

milestone=${version}
if [[ $RELEASE_TYPE = "RELEASE" ]]; then
	milestone=${version%.RELEASE}
fi

java -jar /github-changelog-generator.jar \
  --spring.config.location=${CONFIG_DIR}/changelog-generator.yml \
  ${milestone} generated-changelog/changelog.md

echo ${version} > generated-changelog/version
echo v${version} > generated-changelog/tag
