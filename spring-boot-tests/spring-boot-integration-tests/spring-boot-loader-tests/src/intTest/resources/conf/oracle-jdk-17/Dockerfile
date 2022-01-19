FROM ubuntu:focal-20211006
RUN apt-get update && \
    apt-get install -y software-properties-common curl && \
    mkdir -p /opt/oraclejdk && \
    cd /opt/oraclejdk && \
    curl -L https://download.oracle.com/java/17/latest/jdk-17_linux-x64_bin.tar.gz | tar zx --strip-components=1
ENV JAVA_HOME /opt/oraclejdk
ENV PATH $JAVA_HOME/bin:$PATH
