#!/bin/bash
set -e

source $(dirname $0)/common.sh
repository=$(pwd)/distribution-repository

pushd git-repo > /dev/null
if [[ -d /opt/openjdk-secondary ]]; then
  java_version=$( ./$(dirname $0)/get-secondary-java-version.sh )
  if [[ ${java_version} = "14" ]]; then
	  ./gradlew -Dorg.gradle.internal.launcher.welcomeMessageEnabled=false --no-daemon --max-workers=4 -PtestJavaHome=/opt/openjdk-secondary -PdeploymentRepository=${repository} build publishAllPublicationsToDeploymentRepository -x :spring-boot-project:spring-boot-tools:spring-boot-gradle-plugin:test
  else
	  ./gradlew -Dorg.gradle.internal.launcher.welcomeMessageEnabled=false --no-daemon --max-workers=4 -PtestJavaHome=/opt/openjdk-secondary -PdeploymentRepository=${repository} build publishAllPublicationsToDeploymentRepository
  fi
else
	./gradlew -Dorg.gradle.internal.launcher.welcomeMessageEnabled=false --no-daemon --max-workers=4 -PdeploymentRepository=${repository} build publishAllPublicationsToDeploymentRepository
fi
popd > /dev/null
