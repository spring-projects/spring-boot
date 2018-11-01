# 9.0.4+12-Debian-4
FROM openjdk@sha256:0e48e7e79d9e5637ee5d9c4a4fa2615c7bde69390aab06f7105eb4a8ebec8383

RUN apt-get update && \
    apt-get install -y git && \
    apt-get install -y libxml2-utils && \
    apt-get install -y jq

ADD https://raw.githubusercontent.com/spring-io/concourse-java-scripts/v0.0.2/concourse-java.sh /opt/

ENV DOCKER_VERSION=17.05.0-ce \
    ENTRYKIT_VERSION=0.4.0

RUN apt-get update && \
    apt-get install -y curl && \
	apt-get install -y libudev1 && \
	apt-get install -y iptables && \
    curl https://get.docker.com/builds/Linux/x86_64/docker-${DOCKER_VERSION}.tgz | tar zx && \
    mv /docker/* /bin/ && chmod +x /bin/docker*

# Install entrykit
RUN curl -L https://github.com/progrium/entrykit/releases/download/v${ENTRYKIT_VERSION}/entrykit_${ENTRYKIT_VERSION}_Linux_x86_64.tgz | tar zx && \
    chmod +x entrykit && \
    mv entrykit /bin/entrykit && \
    entrykit --symlink

ADD https://raw.githubusercontent.com/spring-projects/spring-boot/master/ci/images/docker-lib.sh /docker-lib.sh

ENTRYPOINT [ \
	"switch", \
		"shell=/bin/sh", "--", \
	"codep", \
		"/bin/docker daemon" \
]