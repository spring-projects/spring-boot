FROM ubuntu:focal-20210416

ADD setup.sh /setup.sh
ADD get-jdk-url.sh /get-jdk-url.sh
ADD get-docker-url.sh /get-docker-url.sh
RUN ./setup.sh java15

ENV JAVA_HOME /opt/openjdk
ENV PATH $JAVA_HOME/bin:$PATH
ADD docker-lib.sh /docker-lib.sh

ENTRYPOINT [ "switch", "shell=/bin/bash", "--", "codep", "/bin/docker daemon" ]
