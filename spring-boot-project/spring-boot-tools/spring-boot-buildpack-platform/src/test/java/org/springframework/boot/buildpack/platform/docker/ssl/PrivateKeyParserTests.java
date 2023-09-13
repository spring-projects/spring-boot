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

package org.springframework.boot.buildpack.platform.docker.ssl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.boot.buildpack.platform.docker.ssl.PrivateKeyParser.DerEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link PrivateKeyParser}.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
class PrivateKeyParserTests {

	private PemFileWriter fileWriter;

	@BeforeEach
	void setUp() throws IOException {
		this.fileWriter = new PemFileWriter();
	}

	@AfterEach
	void tearDown() throws IOException {
		this.fileWriter.cleanup();
	}

	@Test
	void parsePkcs8RsaKeyFile() throws IOException {
		Path path = this.fileWriter.writeFile("key.pem", PemFileWriter.PKCS8_PRIVATE_RSA_KEY);
		PrivateKey privateKey = PrivateKeyParser.parse(path);
		assertThat(privateKey).isNotNull();
		assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
	}

	@ParameterizedTest
	@ValueSource(strings = { PemFileWriter.PKCS8_PRIVATE_EC_NIST_P256_KEY, PemFileWriter.PKCS8_PRIVATE_EC_NIST_P384_KEY,
			PemFileWriter.PKCS8_PRIVATE_EC_PRIME256V1_KEY, PemFileWriter.PKCS8_PRIVATE_EC_SECP256R1_KEY })
	void parsePkcs8EcKeyFile(String contents) throws IOException {
		Path path = this.fileWriter.writeFile("key.pem", contents);
		PrivateKey privateKey = PrivateKeyParser.parse(path);
		assertThat(privateKey).isNotNull();
		assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
		assertThat(privateKey.getAlgorithm()).isEqualTo("EC");
	}

	@Test
	void parsePkcs8DsaKeyFile() throws IOException {
		Path path = this.fileWriter.writeFile("key.pem", PemFileWriter.PRIVATE_DSA_KEY);
		PrivateKey privateKey = PrivateKeyParser.parse(path);
		assertThat(privateKey).isNotNull();
		assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
		assertThat(privateKey.getAlgorithm()).isEqualTo("DSA");
	}

	@Test
	void parsePkcs1RsaKeyFile() throws IOException {
		Path path = this.fileWriter.writeFile("key.pem", PemFileWriter.PRIVATE_RSA_KEY);
		PrivateKey privateKey = PrivateKeyParser.parse(path);
		assertThat(privateKey).isNotNull();
		assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
	}

	@Test
	void parsePemEcKeyFile() throws IOException {
		Path path = this.fileWriter.writeFile("key.pem", PemFileWriter.PRIVATE_EC_KEY);
		ECPrivateKey privateKey = (ECPrivateKey) PrivateKeyParser.parse(path);
		assertThat(privateKey).isNotNull();
		assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
		assertThat(privateKey.getAlgorithm()).isEqualTo("EC");
		assertThat(privateKey.getParams().toString()).contains("1.3.132.0.34").doesNotContain("prime256v1");
	}

	@Test
	void parsePemEcKeyFilePrime256v1() throws IOException {
		Path path = this.fileWriter.writeFile("key.pem", PemFileWriter.PRIVATE_EC_KEY_PRIME_256_V1);
		ECPrivateKey privateKey = (ECPrivateKey) PrivateKeyParser.parse(path);
		assertThat(privateKey).isNotNull();
		assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
		assertThat(privateKey.getAlgorithm()).isEqualTo("EC");
		assertThat(privateKey.getParams().toString()).contains("prime256v1").doesNotContain("1.3.132.0.34");
	}

	@Test
	void parsePkcs8Ed25519KeyFile() throws IOException {
		Path path = this.fileWriter.writeFile("key.pem", PemFileWriter.PKCS8_PRIVATE_EC_ED25519_KEY);
		PrivateKey privateKey = PrivateKeyParser.parse(path);
		assertThat(privateKey).isNotNull();
		assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
		assertThat(privateKey.getAlgorithm()).isEqualTo("EdDSA");
	}

	@Test
	void parseWithNonKeyFileWillThrowException() throws IOException {
		Path path = this.fileWriter.writeFile("text.pem", "plain text");
		assertThatIllegalStateException().isThrownBy(() -> PrivateKeyParser.parse(path))
			.withMessageContaining(path.toString());
	}

	@Test
	void parseWithInvalidPathWillThrowException() throws URISyntaxException {
		Path path = Paths.get(new URI("file:///bad/path/key.pem"));
		assertThatIllegalStateException().isThrownBy(() -> PrivateKeyParser.parse(path))
			.withMessageContaining(path.toString());
	}

	@Nested
	class DerEncoderTests {

		@Test
		void codeLengthBytesShort() throws Exception {
			DerEncoder encoder = new DerEncoder();
			encoder.codeLengthBytes(0, new byte[127]);
			assertThat(encoder.toByteArray()).startsWith(0x0, 0x7F);
		}

		@Test
		void codeLengthBytesMedium() throws Exception {
			DerEncoder encoder = new DerEncoder();
			encoder.codeLengthBytes(0, new byte[130]);
			assertThat(encoder.toByteArray()).startsWith(0x0, 0x81, 0x82);
		}

		@Test
		void codeLengthBytesLong() throws Exception {
			DerEncoder encoder = new DerEncoder();
			encoder.codeLengthBytes(0, new byte[258]);
			assertThat(encoder.toByteArray()).startsWith(0x0, 0x82, 0x01, 0x02);
		}

	}

}
