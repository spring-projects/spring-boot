#!/bin/bash
set -e

case "$1" in
	java17)
		 echo "https://github.com/bell-sw/Liberica/releases/download/17.0.5+8/bellsoft-jdk17.0.5+8-linux-amd64.tar.gz"
	;;
	java19)
		 echo "https://github.com/bell-sw/Liberica/releases/download/19+37/bellsoft-jdk19+37-linux-amd64.tar.gz"
	;;
	*)
		echo $"Unknown java version"
		exit 1
esac
