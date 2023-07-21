#!/bin/bash

source $(dirname $0)/common.sh

version=$( cat artifactory-repo/build-info.json | jq -r '.buildInfo.modules[0].id' | sed 's/.*:.*:\(.*\)/\1/' )

git-repo/gradlew publishExisting -p git-repo/ci/config/gradle-plugin-publishing -Pgradle.publish.key=${GRADLE_PUBLISH_KEY} -Pgradle.publish.secret=${GRADLE_PUBLISH_SECRET} -PbootVersion=${version} -PrepositoryRoot=$(pwd)/artifactory-repo
