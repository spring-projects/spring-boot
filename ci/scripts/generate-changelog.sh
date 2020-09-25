#!/bin/bash
set -e

version=$( cat version/version )

java -jar /github-changelog-generator.jar \
  --changelog.repository=spring-projects/spring-boot  \
  ${version} generated-changelog/changelog.md

echo ${version} > generated-changelog/version
echo v${version} > generated-changelog/tag
