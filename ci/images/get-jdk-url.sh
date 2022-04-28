#!/bin/bash
set -e

case "$1" in
	java17)
		 echo "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.3%2B7/OpenJDK17U-jdk_x64_linux_hotspot_17.0.3_7.tar.gz"
	;;
	java18)
		 echo "https://github.com/adoptium/temurin18-binaries/releases/download/jdk-18.0.1%2B10/OpenJDK18U-jdk_x64_linux_hotspot_18.0.1_10.tar.gz"
	;;
  *)
		echo $"Unknown java version"
		exit 1
esac
