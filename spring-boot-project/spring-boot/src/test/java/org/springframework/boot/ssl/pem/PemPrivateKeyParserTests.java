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
	void parsePkcs8KeyFileWithEcdsa() throws Exception {
		PrivateKey privateKey = PemPrivateKeyParser.parse(read("test-ec-key.pem"));
		assertThat(privateKey).isNotNull();
		assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
		assertThat(privateKey.getAlgorithm()).isEqualTo("EC");
	}

	@Test
	void parseWithNonKeyTextWillThrowException() {
		assertThatIllegalStateException().isThrownBy(() -> PemPrivateKeyParser.parse(read("test-banner.txt")));
	}

	private String read(String path) throws IOException {
		return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
	}

}
