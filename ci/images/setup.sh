#!/bin/bash
set -ex

###########################################################
# UTILS
###########################################################

apt-get update
apt-get install --no-install-recommends -y ca-certificates net-tools libxml2-utils git curl libudev1 libxml2-utils iptables jq
rm -rf /var/lib/apt/lists/*

curl https://raw.githubusercontent.com/spring-io/concourse-java-scripts/v0.0.2/concourse-java.sh > /opt/concourse-java.sh


###########################################################
# JAVA
###########################################################

case "$1" in
	java8)
		 JDK_URL=https://github.com/AdoptOpenJDK/openjdk8-binaries/releases/download/jdk8u192-b12/OpenJDK8U-jdk_x64_linux_hotspot_8u192b12.tar.gz
		 JDK_STRIP=2
	;;
	java9)
		 JDK_URL=https://download.java.net/java/GA/jdk9/9.0.4/binaries/openjdk-9.0.4_linux-x64_bin.tar.gz
		 JDK_STRIP=1
	;;
	java10)
		 JDK_URL=https://download.java.net/java/GA/jdk10/10.0.1/fb4372174a714e6b8c52526dc134031e/10/openjdk-10.0.1_linux-x64_bin.tar.gz
		 JDK_STRIP=1
	;;
	jav11)
		 JDK_URL=https://download.java.net/java/GA/jdk11/13/GPL/openjdk-11.0.1_linux-x64_bin.tar.gz
		 JDK_STRIP=1
	;;
	*)
		echo $"Unknown java version"
		exit 1
esac
mkdir -p /opt/openjdk
cd /opt/openjdk
curl -L ${JDK_URL} | tar zx --strip-components=${JDK_STRIP}


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
