#!/bin/bash
set -e

case "$1" in
	java8)
		 echo "https://github.com/AdoptOpenJDK/openjdk8-binaries/releases/download/jdk8u282-b08/OpenJDK8U-jdk_x64_linux_hotspot_8u282b08.tar.gz"
	;;
	java11)
		 echo "https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.10%2B9/OpenJDK11U-jdk_x64_linux_hotspot_11.0.10_9.tar.gz"
	;;
	java15)
		 echo "https://github.com/AdoptOpenJDK/openjdk15-binaries/releases/download/jdk-15.0.2%2B7/OpenJDK15U-jdk_x64_linux_hotspot_15.0.2_7.tar.gz"
	;;
	java16)
		 echo "https://github.com/AdoptOpenJDK/openjdk16-binaries/releases/download/jdk16-2021-03-09-12-41/OpenJDK16-jdk_x64_linux_hotspot_2021-03-09-12-41.tar.gz"
	;;
  *)
		echo $"Unknown java version"
		exit 1
esac
