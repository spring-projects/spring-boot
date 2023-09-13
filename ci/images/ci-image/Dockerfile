FROM ubuntu:jammy-20230816

ADD setup.sh /setup.sh
ADD get-jdk-url.sh /get-jdk-url.sh
ADD get-docker-url.sh /get-docker-url.sh
ADD get-docker-compose-url.sh /get-docker-compose-url.sh
RUN ./setup.sh java17

ENV JAVA_HOME /opt/openjdk
ENV PATH $JAVA_HOME/bin:$PATH
ADD docker-lib.sh /docker-lib.sh

ADD build-release-scripts.sh /build-release-scripts.sh
ADD releasescripts /release-scripts
RUN ./build-release-scripts.sh
