#!/bin/bash
set -e

source $(dirname $0)/common.sh
repository=$(pwd)/distribution-repository

pushd git-repo > /dev/null
git fetch --tags --all > /dev/null
popd > /dev/null

git clone git-repo stage-git-repo > /dev/null

pushd stage-git-repo > /dev/null

snapshotVersion=$( awk -F '=' '$1 == "version" { print $2 }' gradle.properties )
if [[ $RELEASE_TYPE = "M" ]]; then
	stageVersion=$( get_next_milestone_release $snapshotVersion)
	nextVersion=$snapshotVersion
elif [[ $RELEASE_TYPE = "RC" ]]; then
	stageVersion=$( get_next_rc_release $snapshotVersion)
	nextVersion=$snapshotVersion
elif [[ $RELEASE_TYPE = "RELEASE" ]]; then
	stageVersion=$( get_next_release $snapshotVersion)
	nextVersion=$( bump_version_number $snapshotVersion)
else
	echo "Unknown release type $RELEASE_TYPE" >&2; exit 1;
fi

echo "Staging $stageVersion (next version will be $nextVersion)"
sed -i "s/version=$snapshotVersion/version=$stageVersion/" gradle.properties

git config user.name "Spring Buildmaster" > /dev/null
git config user.email "buildmaster@springframework.org" > /dev/null
git add gradle.properties > /dev/null
git commit -m"Release v$stageVersion" > /dev/null
git tag -a "v$stageVersion" -m"Release v$stageVersion" > /dev/null

./gradlew --no-daemon --max-workers=4 -PdeploymentRepository=${repository} build publishAllPublicationsToDeploymentRepository

git reset --hard HEAD^ > /dev/null
if [[ $nextVersion != $snapshotVersion ]]; then
	echo "Setting next development version (v$nextVersion)"
	sed -i "s/version=$snapshotVersion/version=$nextVersion/" gradle.properties
	git add gradle.properties > /dev/null
	git commit -m"Next development version (v$nextVersion)" > /dev/null
fi

echo "DONE"

popd > /dev/null
