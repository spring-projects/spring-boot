# CentOS 5.11 from 22/03/2016
FROM centos@sha256:7f8a808416f712da6931ac65e4308fede7fe86bcf15364b6f63af88840fe6131
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
