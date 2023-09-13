#!/bin/bash
set -e

case "$1" in
	java8)
		 echo "https://github.com/bell-sw/Liberica/releases/download/8u382+6/bellsoft-jdk8u382+6-linux-amd64.tar.gz"
	;;
	java11)
		 echo "https://github.com/bell-sw/Liberica/releases/download/11.0.20.1+1/bellsoft-jdk11.0.20.1+1-linux-amd64.tar.gz"
	;;
	java17)
		 echo "https://github.com/bell-sw/Liberica/releases/download/17.0.8.1+1/bellsoft-jdk17.0.8.1+1-linux-amd64.tar.gz"
	;;
	java20)
		 echo "https://github.com/bell-sw/Liberica/releases/download/20.0.2+10/bellsoft-jdk20.0.2+10-linux-amd64.tar.gz"
	;;
	java21)
		 echo "https://download.java.net/java/early_access/jdk21/25/GPL/openjdk-21-ea+25_linux-x64_bin.tar.gz"
	;;
	*)
		echo $"Unknown java version"
		exit 1
esac
