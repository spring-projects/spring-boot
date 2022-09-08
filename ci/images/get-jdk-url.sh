#!/bin/bash
set -e

case "$1" in
	java8)
		 echo "https://github.com/bell-sw/Liberica/releases/download/8u345+1/bellsoft-jdk8u345+1-linux-amd64.tar.gz"
	;;
	java11)
		 echo "https://github.com/bell-sw/Liberica/releases/download/11.0.16.1+1/bellsoft-jdk11.0.16.1+1-linux-amd64.tar.gz"
	;;
	java17)
		 echo "https://github.com/bell-sw/Liberica/releases/download/17.0.4.1+1/bellsoft-jdk17.0.4.1+1-linux-amd64.tar.gz"
	;;
	java18)
		 echo "https://github.com/bell-sw/Liberica/releases/download/18.0.2.1+1/bellsoft-jdk18.0.2.1+1-linux-amd64.tar.gz"
	;;
	java19)
		 echo "https://github.com/adoptium/temurin19-binaries/releases/download/jdk19-2022-09-06-18-04-beta/OpenJDK19U-jdk_x64_linux_hotspot_2022-09-06-18-04.tar.gz"
	;;
	*)
		echo $"Unknown java version"
		exit 1
esac
