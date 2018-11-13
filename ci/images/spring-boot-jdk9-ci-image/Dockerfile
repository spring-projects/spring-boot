FROM ubuntu:bionic-20181018

ADD setup.sh /setup.sh
RUN ./setup.sh java9

ENV JAVA_HOME /opt/openjdk
ENV PATH $JAVA_HOME/bin:$PATH

ENTRYPOINT [ "switch", "shell=/bin/bash", "--", "codep", "/bin/docker daemon" ]
