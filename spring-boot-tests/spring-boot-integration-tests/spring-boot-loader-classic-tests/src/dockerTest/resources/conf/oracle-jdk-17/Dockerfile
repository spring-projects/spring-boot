FROM ubuntu:jammy-20231211.1
RUN apt-get update && \
    apt-get install -y software-properties-common curl && \
    mkdir -p /opt/oraclejdk && \
    cd /opt/oraclejdk && \
    curl -L https://download.oracle.com/java/17/archive/jdk-17_linux-x64_bin.tar.gz | tar zx --strip-components=1
ENV JAVA_HOME /opt/oraclejdk
ENV PATH $JAVA_HOME/bin:$PATH
