#!/bin/bash
set -e

case "$1" in
	java17)
		 echo "https://github.com/bell-sw/Liberica/releases/download/17.0.7+7/bellsoft-jdk17.0.7+7-linux-amd64.tar.gz"
	;;
	java20)
		 echo "https://github.com/bell-sw/Liberica/releases/download/20.0.1+10/bellsoft-jdk20.0.1+10-linux-amd64.tar.gz"
	;;
	*)
		echo $"Unknown java version"
		exit 1
esac
