# CentOS 6.9 from 02/06/2017
FROM centos@sha256:a23bced61701af9a0a758e94229676d9f09996a3ff0f3d26955b06bac8c282e0
RUN yum install -y wget && \
    yum install -y system-config-services && \
    yum install -y curl && \
    wget --output-document jdk.rpm \
        http://cdn.azul.com/zulu/bin/zulu8.21.0.1-jdk8.0.131-linux.x86_64.rpm && \
    yum --nogpg localinstall -y jdk.rpm && \
    rm -f jdk.rpm
