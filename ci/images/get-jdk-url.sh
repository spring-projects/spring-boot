#!/bin/bash
set -e

case "$1" in
	java8)
		 echo "https://github.com/AdoptOpenJDK/openjdk8-binaries/releases/download/jdk8u265-b01/OpenJDK8U-jdk_x64_linux_hotspot_8u265b01.tar.gz"
	;;
	java11)
		 echo "https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.8%2B10/OpenJDK11U-jdk_x64_linux_hotspot_11.0.8_10.tar.gz"
	;;
	java14)
		 echo "https://github.com/AdoptOpenJDK/openjdk14-binaries/releases/download/jdk-14.0.2%2B12/OpenJDK14U-jdk_x64_linux_hotspot_14.0.2_12.tar.gz"
	;;
	java15)
		 echo "https://github.com/AdoptOpenJDK/openjdk15-binaries/releases/download/jdk15-2020-08-10-09-44/OpenJDK15-jdk_x64_linux_hotspot_2020-08-10-09-44.tar.gz"
	;;
  *)
		echo $"Unknown java version"
		exit 1
esac
