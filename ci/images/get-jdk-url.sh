#!/bin/bash
set -e

case "$1" in
	java8)
		 echo "https://github.com/bell-sw/Liberica/releases/download/8u362+9/bellsoft-jdk8u362+9-linux-amd64.tar.gz"
	;;
	java11)
		 echo "https://github.com/bell-sw/Liberica/releases/download/11.0.18+10/bellsoft-jdk11.0.18+10-linux-amd64.tar.gz"
	;;
	java17)
		 echo "https://github.com/bell-sw/Liberica/releases/download/17.0.6+10/bellsoft-jdk17.0.6+10-linux-amd64.tar.gz"
	;;
	java20)
		 echo "https://github.com/bell-sw/Liberica/releases/download/20+37/bellsoft-jdk20+37-linux-amd64.tar.gz"
	;;
	*)
		echo $"Unknown java version"
		exit 1
esac
