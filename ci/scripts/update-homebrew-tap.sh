#!/bin/bash
set -e

version=$( cat artifactory-repo/build-info.json | jq -r '.buildInfo.modules[0].id' | sed 's/.*:.*:\(.*\)/\1/' )

git clone git-repo updated-repo > /dev/null

if [[ $LATEST_GA = true ]]; then
pushd updated-repo > /dev/null
  cd homebrew-tap
  wget https://repo.spring.io/libs-release-local/org/springframework/boot/spring-boot-cli/${version}/spring-boot-cli-${version}-homebrew.rb
  rm spring-boot.rb
  mv spring-boot-cli-*.rb spring-boot.rb
  git config user.name "Spring Buildmaster" > /dev/null
  git config user.email "buildmaster@springframework.org" > /dev/null
  git add spring-boot.rb > /dev/null
  git commit -m "Upgrade to Spring Boot ${version}" > /dev/null
  echo "DONE"
popd > /dev/null
fi