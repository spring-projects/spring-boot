FROM ubuntu:jammy
RUN apt-get update && \
    apt-get install -y software-properties-common curl softhsm2 && \
    mkdir -p /opt/openjdk && \
    cd /opt/openjdk && \
    curl -L https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.1%2B12/OpenJDK17U-jdk_x64_linux_hotspot_17.0.1_12.tar.gz | tar zx --strip-components=1 && \
    # this mkdir fixes old SoftHSMv2 install issue in older Ubuntus: https://github.com/opendnssec/SoftHSMv2/issues/283
    # mkdir -p /var/lib/softhsm/tokens && \
    echo "name = SoftHSM\nlibrary = /usr/lib/softhsm/libsofthsm2.so\nslotListIndex = 0" > /pkcs11.cfg && \
    echo "security.provider.12=SunPKCS11 /pkcs11.cfg" > /java.security.override

ENV JAVA_HOME /opt/openjdk
ENV PATH $JAVA_HOME/bin:$PATH
ENV JAVA_OPTS "-Djava.security.properties=/java.security.override -Djava.security.debug=sunpkcs11 -Djava.security.debug=pkcs11keystore"