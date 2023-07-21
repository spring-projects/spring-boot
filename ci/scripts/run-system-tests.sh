#!/bin/bash
set -e

source $(dirname $0)/common.sh

pushd git-repo > /dev/null
if [[ -d /opt/openjdk-toolchain ]]; then
  ./gradlew -Dorg.gradle.internal.launcher.welcomeMessageEnabled=false --no-daemon --max-workers=4 systemTest -PtoolchainVersion=${TOOLCHAIN_JAVA_VERSION} -Porg.gradle.java.installations.auto-detect=false -Porg.gradle.java.installations.auto-download=false -Porg.gradle.java.installations.paths=/opt/openjdk-toolchain/
else
  ./gradlew -Dorg.gradle.internal.launcher.welcomeMessageEnabled=false --no-daemon --max-workers=4 systemTest
fi
popd > /dev/null
