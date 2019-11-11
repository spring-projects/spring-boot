#!/bin/bash

export BUILD_INFO_LOCATION=$(pwd)/artifactory-repo/build-info.json

java -jar /spring-boot-release-scripts.jar syncToCentral "RELEASE" $BUILD_INFO_LOCATION > /dev/null || { exit 1; }

echo "Sync complete"
echo $version > version/version
