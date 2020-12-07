# CentOS 7.9 from 18/11/2020
FROM centos@sha256:27525fe9e8a84f95baf88459070124628bf83da7216052ea9365fe46e93a102f
RUN yum install -y wget && \
    yum install -y curl && \
    wget --output-document jdk.rpm \
        https://cdn.azul.com/zulu/bin/zulu8.21.0.1-jdk8.0.131-linux.x86_64.rpm && \
    yum --nogpg localinstall -y jdk.rpm && \
    rm -f jdk.rpm
