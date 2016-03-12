FROM centos:5.11
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
