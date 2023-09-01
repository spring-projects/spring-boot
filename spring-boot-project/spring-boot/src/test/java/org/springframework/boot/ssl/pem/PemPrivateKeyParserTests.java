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

	@Test
	void parsePkcs8RsaKeyFile() throws Exception {
		PrivateKey privateKey = PemPrivateKeyParser.parse(read("ssl/pkcs8/key-rsa.pem"));
		assertThat(privateKey).isNotNull();
		assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
		assertThat(privateKey.getAlgorithm()).isEqualTo("RSA");
	}

	@ParameterizedTest
	@ValueSource(strings = { "key-ec-nist-p256.pem", "key-ec-nist-p384.pem", "key-ec-prime256v1.pem",
			"key-ec-secp256r1.pem" })
	void parsePkcs8EcKeyFile(String fileName) throws Exception {
		PrivateKey privateKey = PemPrivateKeyParser.parse(read("ssl/pkcs8/" + fileName));
		assertThat(privateKey).isNotNull();
		assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
		assertThat(privateKey.getAlgorithm()).isEqualTo("EC");
	}

	@Test
	void parsePkcs8DsaKeyFile() throws Exception {
		PrivateKey privateKey = PemPrivateKeyParser.parse(read("ssl/pkcs8/key-dsa.pem"));
		assertThat(privateKey).isNotNull();
		assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
		assertThat(privateKey.getAlgorithm()).isEqualTo("DSA");
	}

	@Test
	void parsePkcs8Ed25519KeyFile() throws Exception {
		PrivateKey privateKey = PemPrivateKeyParser.parse(read("ssl/pkcs8/key-ec-ed25519.pem"));
		assertThat(privateKey).isNotNull();
		assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
		assertThat(privateKey.getAlgorithm()).isEqualTo("EdDSA");
	}

	@Test
	void parsePemKeyFileWithEcdsa() throws Exception {
		ECPrivateKey privateKey = (ECPrivateKey) PemPrivateKeyParser.parse(read("test-ec-key.pem"));
		assertThat(privateKey).isNotNull();
		assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
		assertThat(privateKey.getAlgorithm()).isEqualTo("EC");
		assertThat(privateKey.getParams().toString()).contains("1.3.132.0.34").doesNotContain("prime256v1");
	}

	@Test
	void parsePemKeyFileWithEcdsaPrime256v1() throws Exception {
		ECPrivateKey privateKey = (ECPrivateKey) PemPrivateKeyParser.parse(read("test-ec-key-prime256v1.pem"));
		assertThat(privateKey).isNotNull();
		assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
		assertThat(privateKey.getAlgorithm()).isEqualTo("EC");
		assertThat(privateKey.getParams().toString()).contains("prime256v1").doesNotContain("1.3.132.0.34");
	}

	@Test
	void parseWithNonKeyTextWillThrowException() {
		assertThatIllegalStateException().isThrownBy(() -> PemPrivateKeyParser.parse(read("test-banner.txt")));
	}

	@Test
	void parsePkcs8EncryptedRsaKeyFile() throws Exception {
		// created with:
		// openssl genpkey -aes-256-cbc -algorithm RSA \
		// -pkeyopt rsa_keygen_bits:4096 -out key-rsa-encrypted.key
		PrivateKey privateKey = PemPrivateKeyParser.parse(read("ssl/pkcs8/key-rsa-encrypted.pem"), "test");
		assertThat(privateKey).isNotNull();
		assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
		assertThat(privateKey.getAlgorithm()).isEqualTo("RSA");
	}

	@Test
	void parsePkcs8EncryptedEcKeyFile() throws Exception {
		// created with:
		// openssl genpkey -aes-256-cbc -algorithm EC \
		// -pkeyopt ec_paramgen_curve:prime256v1 -out key-ec-encrypted.key
		PrivateKey privateKey = PemPrivateKeyParser.parse(read("ssl/pkcs8/key-ec-encrypted.pem"), "test");
		assertThat(privateKey).isNotNull();
		assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
		assertThat(privateKey.getAlgorithm()).isEqualTo("EC");
	}

	@Test
	void failParsingPkcs1EncryptedKeyFile() throws Exception {
		// created with:
		// openssl genrsa -aes-256-cbc -out key-rsa-encrypted.pem
		assertThatIllegalStateException()
			.isThrownBy(() -> PemPrivateKeyParser.parse(read("ssl/pkcs1/key-rsa-encrypted.pem"), "test"))
			.withMessageContaining("Unrecognized private key format");
	}

	@Test
	void failParsingEcEncryptedKeyFile() throws Exception {
		// created with:
		// openssl ecparam -genkey -name prime256v1 | openssl ec -aes-128-cbc -out
		// key-ec-prime256v1-encrypted.pem
		assertThatIllegalStateException()
			.isThrownBy(() -> PemPrivateKeyParser.parse(read("ssl/ec/key-ec-prime256v1-encrypted.pem"), "test"))
			.withMessageContaining("Unrecognized private key format");
	}

	private String read(String path) throws IOException {
		return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
	}

}
