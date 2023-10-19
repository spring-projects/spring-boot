#!/bin/bash
set -e

case "$1" in
	java8)
		 echo "https://github.com/bell-sw/Liberica/releases/download/8u392+9/bellsoft-jdk8u392+9-linux-amd64.tar.gz"
	;;
	java11)
		 echo "https://github.com/bell-sw/Liberica/releases/download/11.0.21+10/bellsoft-jdk11.0.21+10-linux-amd64.tar.gz"
	;;
	java17)
		 echo "https://github.com/bell-sw/Liberica/releases/download/17.0.9+11/bellsoft-jdk17.0.9+11-linux-amd64.tar.gz"
	;;
	java21)
		 echo "https://github.com/bell-sw/Liberica/releases/download/21.0.1+12/bellsoft-jdk21.0.1+12-linux-amd64.tar.gz"
	;;
	*)
		echo $"Unknown java version"
		exit 1
esac
