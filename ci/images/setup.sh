#!/bin/bash
set -ex

###########################################################
# UTILS
###########################################################

apt-get update
apt-get install --no-install-recommends -y ca-certificates net-tools libxml2-utils git curl libudev1 libxml2-utils iptables iproute2 jq
rm -rf /var/lib/apt/lists/*

curl https://raw.githubusercontent.com/spring-io/concourse-java-scripts/v0.0.2/concourse-java.sh > /opt/concourse-java.sh


###########################################################
# JAVA
###########################################################
JDK_URL=$( ./get-jdk-url.sh $1 )
case "$1" in
	java8)
		 COMPONENTS=2
	;;
	java11)
		 COMPONENTS=1
	;;
	*)
		echo $"Unknown java version"
		exit 1
esac
mkdir -p /opt/openjdk
cd /opt/openjdk
curl -L ${JDK_URL} | tar zx --strip-components=${COMPONENTS}
test -f /opt/openjdk/bin/java


###########################################################
# DOCKER
###########################################################

cd /
curl -L https://download.docker.com/linux/static/stable/x86_64/docker-18.06.1-ce.tgz | tar zx
mv /docker/* /bin/
chmod +x /bin/docker*

export ENTRYKIT_VERSION=0.4.0
curl -L https://github.com/progrium/entrykit/releases/download/v${ENTRYKIT_VERSION}/entrykit_${ENTRYKIT_VERSION}_Linux_x86_64.tgz | tar zx
chmod +x entrykit && \
mv entrykit /bin/entrykit && \
entrykit --symlink
