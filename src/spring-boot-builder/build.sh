#!/usr/bin/env bash
set -x -eo pipefail

docker pull paketobuildpacks/build:tiny-cnb
docker tag paketobuildpacks/build:tiny-cnb projects.registry.vmware.com/springboot/build:tiny-cnb
docker push projects.registry.vmware.com/springboot/build:tiny-cnb

docker pull paketobuildpacks/run:tiny-cnb
docker tag paketobuildpacks/run:tiny-cnb projects.registry.vmware.com/springboot/run:tiny-cnb
docker push projects.registry.vmware.com/springboot/run:tiny-cnb

cd builder
pack builder create projects.registry.vmware.com/springboot/spring-boot-cnb-builder:0.0.1 --config builder.toml
docker push projects.registry.vmware.com/springboot/spring-boot-cnb-builder:0.0.1
cd -