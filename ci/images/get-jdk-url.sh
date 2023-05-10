#!/bin/bash
set -e

case "$1" in
	java8)
		 echo "https://github.com/bell-sw/Liberica/releases/download/8u372+7/bellsoft-jdk8u372+7-linux-amd64.tar.gz"
	;;
	java11)
		 echo "https://github.com/bell-sw/Liberica/releases/download/11.0.19+7/bellsoft-jdk11.0.19+7-linux-amd64.tar.gz"
	;;
	java17)
		 echo "https://github.com/bell-sw/Liberica/releases/download/17.0.7+7/bellsoft-jdk17.0.7+7-linux-amd64.tar.gz"
	;;
	java18)
		 echo "https://github.com/bell-sw/Liberica/releases/download/18.0.2.1+1/bellsoft-jdk18.0.2.1+1-linux-amd64.tar.gz"
	;;
	*)
		echo $"Unknown java version"
		exit 1
esac
