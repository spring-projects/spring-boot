#!/bin/bash

source $(dirname $0)/common.sh

version=$( cat artifactory-repo/build-info.json | jq -r '.buildInfo.modules[0].id' | sed 's/.*:.*:\(.*\)/\1/' )

java -jar /spring-boot-release-scripts.jar publishToSdkman $RELEASE_TYPE $version $LATEST_GA || { exit 1; }

echo "Push to SDKMAN complete"
