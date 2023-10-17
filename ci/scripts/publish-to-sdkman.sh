#!/bin/bash

CONFIG_DIR=git-repo/ci/config

version=$( cat artifactory-repo/build-info.json | jq -r '.buildInfo.modules[0].id' | sed 's/.*:.*:\(.*\)/\1/' )

java -jar /concourse-release-scripts.jar \
  --spring.config.location=${CONFIG_DIR}/release-scripts.yml \
  publishToSdkman $RELEASE_TYPE $version $LATEST_GA || { exit 1; }

echo "Push to SDKMAN complete"
