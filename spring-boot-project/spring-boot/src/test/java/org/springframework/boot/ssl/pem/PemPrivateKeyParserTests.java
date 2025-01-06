/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.ssl.pem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link PemPrivateKeyParser}.
 *
 * @author Scott Frederick
 * @author Moritz Halbritter
 * @author Phillip Webb
 */
class PemPrivateKeyParserTests {

	@ParameterizedTest
	// @formatter:off
	@CsvSource({
			"dsa.key,		DSA",
			"rsa.key,		RSA",
			"rsa-pss.key,	RSASSA-PSS"
	})
		// @formatter:on
	void shouldParseTraditionalPkcs8(String file, String algorithm) throws IOException {
		PrivateKey privateKey = PemPrivateKeyParser.parse(read("org/springframework/boot/web/server/pkcs8/" + file));
		assertThat(privateKey).isNotNull();
		assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
		assertThat(privateKey.getAlgorithm()).isEqualTo(algorithm);
	}

	@ParameterizedTest
	// @formatter:off
	@CsvSource({
			"rsa.key,	RSA"
	})
		// @formatter:on
	void shouldParseTraditionalPkcs1(String file, String algorithm) throws IOException {
		PrivateKey privateKey = PemPrivateKeyParser.parse(read("org/springframework/boot/web/server/pkcs1/" + file));
		assertThat(privateKey).isNotNull();
		assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
		assertThat(privateKey.getAlgorithm()).isEqualTo(algorithm);
	}

	@ParameterizedTest
	// @formatter:off
	@ValueSource(strings = {
			"dsa.key"
	})
		// @formatter:on
	void shouldNotParseUnsupportedTraditionalPkcs1(String file) {
		assertThatIllegalStateException()
			.isThrownBy(() -> PemPrivateKeyParser.parse(read("org/springframework/boot/web/server/pkcs1/" + file)))
			.withMessageContaining("Missing private key or unrecognized format");
	}

	@ParameterizedTest
	// @formatter:off
	@CsvSource({
			"brainpoolP256r1.key,	brainpoolP256r1,	1.3.36.3.3.2.8.1.1.7",
			"brainpoolP320r1.key,	brainpoolP320r1,	1.3.36.3.3.2.8.1.1.9",
			"brainpoolP384r1.key,	brainpoolP384r1,	1.3.36.3.3.2.8.1.1.11",
			"brainpoolP512r1.key,	brainpoolP512r1,	1.3.36.3.3.2.8.1.1.13",
			"prime256v1.key,		secp256r1,			1.2.840.10045.3.1.7",
			"secp224r1.key,			secp224r1,			1.3.132.0.33",
			"secp256k1.key,			secp256k1,			1.3.132.0.10",
			"secp256r1.key,			secp256r1,			1.2.840.10045.3.1.7",
			"secp384r1.key,			secp384r1,			1.3.132.0.34",
			"secp521r1.key,			secp521r1,			1.3.132.0.35"
	})
		// @formatter:on
	void shouldParseEcPkcs8(String file, String curveName, String oid) throws IOException {
		PrivateKey privateKey = PemPrivateKeyParser.parse(read("org/springframework/boot/web/server/pkcs8/" + file));
		assertThat(privateKey).isNotNull();
		assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
		assertThat(privateKey.getAlgorithm()).isEqualTo("EC");
		assertThat(privateKey).isInstanceOf(ECPrivateKey.class);
		ECPrivateKey ecPrivateKey = (ECPrivateKey) privateKey;
		assertThat(ecPrivateKey.getParams().toString()).contains(curveName).contains(oid);
	}

	@ParameterizedTest
	// @formatter:off
	@ValueSource(strings = {
			"brainpoolP256t1.key",
			"brainpoolP320t1.key",
			"brainpoolP384t1.key",
			"brainpoolP512t1.key"
	})
		// @formatter:on
	void shouldNotParseUnsupportedEcPkcs8(String file) {
		assertThatIllegalStateException()
			.isThrownBy(() -> PemPrivateKeyParser.parse(read("org/springframework/boot/web/server/pkcs8/" + file)))
			.withMessageContaining("Missing private key or unrecognized format");
	}

	@ParameterizedTest
	// @formatter:off
	@ValueSource(strings = {
			"ed448.key",
			"ed25519.key"
	})
		// @formatter:on
	void shouldParseEdDsaPkcs8(String file) throws IOException {
		PrivateKey privateKey = PemPrivateKeyParser.parse(read("org/springframework/boot/web/server/pkcs8/" + file));
		assertThat(privateKey).isNotNull();
		assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
		assertThat(privateKey.getAlgorithm()).isEqualTo("EdDSA");
	}

	@ParameterizedTest
	// @formatter:off
	@ValueSource(strings = {
			"x448.key",
			"x25519.key"
	})
		// @formatter:on
	void shouldParseXdhPkcs8(String file) throws IOException {
		PrivateKey privateKey = PemPrivateKeyParser.parse(read("org/springframework/boot/web/server/pkcs8/" + file));
		assertThat(privateKey).isNotNull();
		assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
		assertThat(privateKey.getAlgorithm()).isEqualTo("XDH");
	}

	@ParameterizedTest
	// @formatter:off
	@CsvSource({
			"brainpoolP256r1.key,	brainpoolP256r1,	1.3.36.3.3.2.8.1.1.7",
			"brainpoolP320r1.key,	brainpoolP320r1,	1.3.36.3.3.2.8.1.1.9",
			"brainpoolP384r1.key,	brainpoolP384r1,	1.3.36.3.3.2.8.1.1.11",
			"brainpoolP512r1.key,	brainpoolP512r1,	1.3.36.3.3.2.8.1.1.13",
			"prime256v1.key,		secp256r1,			1.2.840.10045.3.1.7",
			"secp224r1.key,			secp224r1,			1.3.132.0.33",
			"secp256k1.key,			secp256k1,			1.3.132.0.10",
			"secp256r1.key,			secp256r1,			1.2.840.10045.3.1.7",
			"secp384r1.key,			secp384r1,			1.3.132.0.34",
			"secp521r1.key,			secp521r1,			1.3.132.0.35"
	})
		// @formatter:on
	void shouldParseEcSec1(String file, String curveName, String oid) throws IOException {
		PrivateKey privateKey = PemPrivateKeyParser.parse(read("org/springframework/boot/web/server/sec1/" + file));
		assertThat(privateKey).isNotNull();
		assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
		assertThat(privateKey.getAlgorithm()).isEqualTo("EC");
		assertThat(privateKey).isInstanceOf(ECPrivateKey.class);
		ECPrivateKey ecPrivateKey = (ECPrivateKey) privateKey;
		assertThat(ecPrivateKey.getParams().toString()).contains(curveName).contains(oid);
	}

	@ParameterizedTest
	// @formatter:off
	@ValueSource(strings = {
			"brainpoolP256t1.key",
			"brainpoolP320t1.key",
			"brainpoolP384t1.key",
			"brainpoolP512t1.key"
	})
		// @formatter:on
	void shouldNotParseUnsupportedEcSec1(String file) {
		assertThatIllegalStateException()
			.isThrownBy(() -> PemPrivateKeyParser.parse(read("org/springframework/boot/web/server/sec1/" + file)))
			.withMessageContaining("Missing private key or unrecognized format");
	}

	@Test
	void parseWithNonKeyTextWillThrowException() {
		assertThatIllegalStateException().isThrownBy(() -> PemPrivateKeyParser.parse(read("test-banner.txt")));
	}

	@ParameterizedTest
	// @formatter:off
	@CsvSource({
			"dsa-aes-128-cbc.key,				DSA",
			"rsa-aes-256-cbc.key,				RSA",
			"prime256v1-aes-256-cbc.key,		EC",
			"ed25519-aes-256-cbc.key,			EdDSA",
			"x448-aes-256-cbc.key,				XDH"
	})
		// @formatter:on
	void shouldParseEncryptedPkcs8(String file, String algorithm) throws IOException {
		// Created with:
		// openssl pkcs8 -topk8 -in <input file> -out <output file> -v2 <algorithm>
		// -passout pass:test
		// where <algorithm> is aes128 or aes256
		PrivateKey privateKey = PemPrivateKeyParser.parse(read("org/springframework/boot/web/server/pkcs8/" + file),
				"test");
		assertThat(privateKey).isNotNull();
		assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
		assertThat(privateKey.getAlgorithm()).isEqualTo(algorithm);
	}

	@Test
	void shouldNotParseEncryptedPkcs8NotUsingAes() {
		// Created with:
		// openssl pkcs8 -topk8 -in rsa.key -out rsa-des-ede3-cbc.key -v2 des3 -passout
		// pass:test
		assertThatIllegalStateException()
			.isThrownBy(() -> PemPrivateKeyParser
				.parse(read("org/springframework/boot/web/server/pkcs8/rsa-des-ede3-cbc.key"), "test"))
			.isInstanceOf(IllegalStateException.class)
			.withMessageContaining("Error decrypting private key");
	}

	@Test
	void shouldNotParseEncryptedPkcs8NotUsingPbkdf2() {
		// Created with:
		// openssl pkcs8 -topk8 -in rsa.key -out rsa-des-ede3-cbc.key -scrypt -passout
		// pass:test
		assertThatIllegalStateException()
			.isThrownBy(() -> PemPrivateKeyParser
				.parse(read("org/springframework/boot/web/server/pkcs8/rsa-scrypt.key"), "test"))
			.withMessageContaining("Error decrypting private key");
	}

	@Test
	void shouldNotParseEncryptedSec1() {
		// created with:
		// openssl ecparam -genkey -name prime256v1 | openssl ec -aes-128-cbc -out
		// prime256v1-aes-128-cbc.key
		assertThatIllegalStateException()
			.isThrownBy(() -> PemPrivateKeyParser
				.parse(read("org/springframework/boot/web/server/sec1/prime256v1-aes-128-cbc.key"), "test"))
			.withMessageContaining("Missing private key or unrecognized format");
	}

	@Test
	void shouldNotParseEncryptedPkcs1() {
		// created with:
		// openssl genrsa -aes-256-cbc -out rsa-aes-256-cbc.key
		assertThatIllegalStateException()
			.isThrownBy(() -> PemPrivateKeyParser
				.parse(read("org/springframework/boot/web/server/pkcs1/rsa-aes-256-cbc.key"), "test"))
			.withMessageContaining("Missing private key or unrecognized format");
	}

	private String read(String path) throws IOException {
		return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
	}

}
