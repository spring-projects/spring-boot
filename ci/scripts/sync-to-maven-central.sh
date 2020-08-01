#!/bin/bash

export BUILD_INFO_LOCATION=$(pwd)/artifactory-repo/build-info.json
version=$( cat artifactory-repo/build-info.json | jq -r '.buildInfo.modules[0].id' | sed 's/.*:.*:\(.*\)/\1/' )
java -jar /spring-boot-release-scripts.jar syncToCentral "RELEASE" $BUILD_INFO_LOCATION || { exit 1; }

echo "Sync complete"
echo $version > version/version
