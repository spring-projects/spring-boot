FROM openjdk:9-b179-jdk

RUN apt-get update && \
    apt-get install -y git && \
    apt-get install -y libxml2-utils && \
    apt-get install -y jq

ADD https://raw.githubusercontent.com/spring-io/concourse-java-scripts/v0.0.1/concourse-java.sh /opt/