#!/bin/bash
set -ex

pushd /release-scripts
	./mvnw clean install
popd
cp /release-scripts/target/spring-boot-release-scripts.jar .