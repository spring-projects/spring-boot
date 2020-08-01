FROM ubuntu:trusty-20160914
RUN apt-get update && \
    apt-get install -y software-properties-common curl && \
    mkdir -p /opt/openjdk && \
    cd /opt/openjdk && \
    curl -L https://github.com/AdoptOpenJDK/openjdk8-binaries/releases/download/jdk8u202-b08/OpenJDK8U-jdk_x64_linux_hotspot_8u202b08.tar.gz | tar zx --strip-components=1
ENV JAVA_HOME /opt/openjdk
ENV PATH $JAVA_HOME/bin:$PATH
ADD app.jar /app.jar
ADD test-functions.sh /test-functions.sh