# CentOS 7.9 from 18/11/2020
FROM centos@sha256:e4ca2ed0202e76be184e75fb26d14bf974193579039d5573fb2348664deef76e
RUN yum install -y wget && \
    yum install -y curl && \
    wget --output-document jdk.rpm \
        https://cdn.azul.com/zulu/bin/zulu8.21.0.1-jdk8.0.131-linux.x86_64.rpm && \
    yum --nogpg localinstall -y jdk.rpm && \
    rm -f jdk.rpm
