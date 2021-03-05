#!/bin/bash
set -e

/opt/openjdk-toolchain/bin/java -XshowSettings:properties -version 2>&1 | grep "java.specification.version" | awk '{split($0,parts,"="); print parts[2]}' | awk '{$1=$1;print}'
