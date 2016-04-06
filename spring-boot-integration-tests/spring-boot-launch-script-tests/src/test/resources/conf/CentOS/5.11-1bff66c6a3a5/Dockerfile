# this Dockerfile uses digest of centos:5.11 image dated Mar 22 2016 to ensure the tests
# are always executed using the same image since release-named tags (such as 5.11) are
# actually updated over the time, making the build not repeatable
#
# see gh-5397, https://hub.docker.com/r/library/centos/tags/

FROM centos@sha256:7f8a808416f712da6931ac65e4308fede7fe86bcf15364b6f63af88840fe6131
RUN yum install -y wget && \
    yum install -y system-config-services && \
    yum install -y curl && \
    wget --no-cookies \
        --no-check-certificate \
        --header  "Cookie: gpw_e24=http%3A%2F%2Fwww.oracle.com%2F; oraclelicense=accept-securebackup-cookie" \
        --output-document jdk.rpm \
        http://download.oracle.com/otn-pub/java/jdk/8u66-b17/jdk-8u66-linux-x64.rpm && \
    yum --nogpg localinstall -y jdk.rpm && \
    rm -f jdk.rpm
