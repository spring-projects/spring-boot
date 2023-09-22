#!/bin/bash
set -e

case "$1" in
	java17)
		 echo "https://github.com/bell-sw/Liberica/releases/download/17.0.8.1+1/bellsoft-jdk17.0.8.1+1-linux-amd64.tar.gz"
	;;
	java21)
		 echo "https://github.com/bell-sw/Liberica/releases/download/21+37/bellsoft-jdk21+37-linux-amd64.tar.gz"
	;;
	*)
		echo $"Unknown java version"
		exit 1
esac
