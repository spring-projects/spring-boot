# CentOS 7.9 from 18/11/2020
FROM centos@sha256:e4ca2ed0202e76be184e75fb26d14bf974193579039d5573fb2348664deef76e
RUN mkdir -p /opt/openjdk && \
    cd /opt/openjdk && \
    curl -L https://github.com/AdoptOpenJDK/openjdk8-binaries/releases/download/jdk8u202-b08/OpenJDK8U-jdk_x64_linux_hotspot_8u202b08.tar.gz | tar zx --strip-components=1
ENV JAVA_HOME /opt/openjdk
ENV PATH $JAVA_HOME/bin:$PATH
