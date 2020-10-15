#!/bin/bash
set -e

CONFIG_DIR=git-repo/ci/config
version=$( cat version/version )

java -jar /github-changelog-generator.jar \
  --spring.config.location=${CONFIG_DIR}/changelog-generator.yml \
  ${version} generated-changelog/changelog.md

echo ${version} > generated-changelog/version
echo v${version} > generated-changelog/tag
