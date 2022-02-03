#!/bin/bash
set -e

case "$1" in
	java17)
		 echo "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.2%2B8/OpenJDK17U-jdk_x64_linux_hotspot_17.0.2_8.tar.gz"
	;;
  *)
		echo $"Unknown java version"
		exit 1
esac
