#!/usr/bin/env bash



openssl pkcs8 -topk8 -in brainpoolP256r1.key -nocrypt -out ../pkcs8/brainpoolP256r1.key
openssl pkcs8 -topk8 -in brainpoolP256t1.key -nocrypt -out ../pkcs8/brainpoolP256t1.key
openssl pkcs8 -topk8 -in brainpoolP320r1.key -nocrypt -out ../pkcs8/brainpoolP320r1.key
openssl pkcs8 -topk8 -in brainpoolP320t1.key -nocrypt -out ../pkcs8/brainpoolP320t1.key
openssl pkcs8 -topk8 -in brainpoolP384r1.key -nocrypt -out ../pkcs8/brainpoolP384r1.key
openssl pkcs8 -topk8 -in brainpoolP384t1.key -nocrypt -out ../pkcs8/brainpoolP384t1.key
openssl pkcs8 -topk8 -in brainpoolP512r1.key -nocrypt -out ../pkcs8/brainpoolP512r1.key
openssl pkcs8 -topk8 -in brainpoolP512t1.key -nocrypt -out ../pkcs8/brainpoolP512t1.key
openssl pkcs8 -topk8 -in prime256v1.key -nocrypt -out ../pkcs8/prime256v1.key
openssl pkcs8 -topk8 -in secp224r1.key -nocrypt -out ../pkcs8/secp224r1.key
