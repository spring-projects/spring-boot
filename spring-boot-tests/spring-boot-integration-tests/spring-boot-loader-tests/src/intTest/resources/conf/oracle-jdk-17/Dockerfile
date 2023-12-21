FROM ubuntu:jammy-20231211.1 as prepare
COPY downloads/* /opt/download/
RUN mkdir -p /opt/jdk && \
    cd /opt/jdk && \
    tar xzf  /opt/download/* --strip-components=1

FROM ubuntu:jammy-20231211.1
COPY --from=prepare /opt/jdk /opt/jdk
ENV JAVA_HOME /opt/jdk
ENV PATH $JAVA_HOME/bin:$PATH
