#!/bin/bash

source $(dirname $0)/common.sh

export BUILD_INFO_LOCATION=$(pwd)/artifactory-repo/build-info.json

java -jar /spring-boot-release-scripts.jar promote $RELEASE_TYPE $BUILD_INFO_LOCATION > /dev/null || { exit 1; }

java -jar /spring-boot-release-scripts.jar distribute $RELEASE_TYPE $BUILD_INFO_LOCATION > /dev/null || { exit 1; }

java -jar /spring-boot-release-scripts.jar publishGradlePlugin $RELEASE_TYPE $BUILD_INFO_LOCATION > /dev/null || { exit 1; }

echo "Promotion complete"
echo $version > version/version
