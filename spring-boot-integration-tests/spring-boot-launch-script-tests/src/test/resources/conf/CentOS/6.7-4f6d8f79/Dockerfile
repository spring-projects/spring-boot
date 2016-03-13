# CentOS 6.7 from 22/03/2016
FROM centos@sha256:4f6d8f794af3574eca603b965fc0b63fdf852be29c17d3ab4cad7ec2272b82bd
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
