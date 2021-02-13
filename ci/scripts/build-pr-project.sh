#!/bin/bash
set -e

source $(dirname $0)/common.sh

pushd git-repo > /dev/null
./gradlew --no-daemon --max-workers=4 --continue build
popd > /dev/null
