#!/bin/bash

create_ssl_config() {
    cat > openssl.cnf <<_END_
    subjectAltName = @alt_names
    [alt_names]
    DNS.1 = example.com
    DNS.2 = localhost
    [ server_cert ]
    keyUsage = digitalSignature, keyEncipherment
    nsCertType = server
    [ client_cert ]
    keyUsage = digitalSignature, keyEncipherment
    nsCertType = client
_END_

}

generate_ca_cert() {
    local location=$1

    mkdir -p ${location}

    openssl genrsa -out ${location}/test-ca.key 4096
    openssl req -key ${location}/test-ca.key -out ${location}/test-ca.crt \
        -x509 -new -nodes -sha256 -days 3650 \
        -subj "/O=Spring Boot Test/CN=Certificate Authority" \
        -addext "subjectAltName=DNS:hello.example.com,DNS:hello-alt.example.com"
}

generate_cert() {
    local location=$1
    local caLocation=$2
    local hostname=$3

    local keyfile=${location}/test-${hostname}-server.key
    local certfile=${location}/test-${hostname}-server.crt

    mkdir -p ${location}

    openssl genrsa -out ${keyfile} 2048
    openssl req -key ${keyfile} \
        -new -sha256 \
        -subj "/O=Spring Boot Test/CN=${hostname}.example.com" \
        -addext "subjectAltName=DNS:${hostname}.example.com" | \
    openssl x509 -req -out ${certfile} \
        -CA ${caLocation}/test-ca.crt -CAkey ${caLocation}/test-ca.key -CAserial ${caLocation}/test-ca.txt -CAcreateserial \
        -sha256 -days 3650 \
        -extfile openssl.cnf \
        -extensions server_cert
}

if ! command -v openssl &> /dev/null; then
    echo "openssl is required"
    exit
fi

mkdir -p certs

create_ssl_config
generate_ca_cert certs/ca
generate_cert certs/default certs/ca hello
generate_cert certs/alt certs/ca hello-alt

rm -f openssl.cnf
rm -f certs/ca/test-ca.key certs/ca/test-ca.txt

cp -r certs/* spring-boot-sni-reactive-app/src/main/resources
cp -r certs/* spring-boot-sni-servlet-app/src/main/resources
cp -r certs/ca/* spring-boot-sni-client-app/src/main/resources/ca

rm -rf certs
