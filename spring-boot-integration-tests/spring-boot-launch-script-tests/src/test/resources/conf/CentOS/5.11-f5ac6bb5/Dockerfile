# CentOS 5.11 from 30/08/2016
FROM centos@sha256:f5ac6bb54c5042387c79c83fca93eff7b721da54ab995fd64a20b23f6785465d
RUN yum install -y wget && \
    yum install -y system-config-services && \
    yum install -y curl && \
    wget --no-cookies \
        --no-check-certificate \
        --header  "Cookie: gpw_e24=http%3A%2F%2Fwww.oracle.com%2F; oraclelicense=accept-securebackup-cookie" \
        --output-document jdk.rpm \
        http://download.oracle.com/otn-pub/java/jdk/8u66-b17/jdk-8u66-linux-x64.rpm && \
    yum --nogpg localinstall -y jdk.rpm && \
    rm -f jdk.rpm
