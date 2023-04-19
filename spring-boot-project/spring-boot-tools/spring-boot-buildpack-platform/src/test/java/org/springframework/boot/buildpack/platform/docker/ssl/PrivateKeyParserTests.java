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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.docker.ssl.PrivateKeyParser.DerEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link PrivateKeyParser}.
 *
 * @author Scott Frederick
 * @author Phillip Webb
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
	void parsePkcs8KeyFile() throws IOException {
		Path path = this.fileWriter.writeFile("key.pem", PemFileWriter.CA_PRIVATE_KEY);
		PrivateKey privateKey = PrivateKeyParser.parse(path);
		assertThat(privateKey).isNotNull();
		assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
		Files.delete(path);
	}

	@Test
	void parsePkcs1RsaKeyFile() throws IOException {
		Path path = this.fileWriter.writeFile("key.pem", PemFileWriter.PRIVATE_RSA_KEY);
		PrivateKey privateKey = PrivateKeyParser.parse(path);
		assertThat(privateKey).isNotNull();
		// keys in PKCS#1 format are converted to PKCS#8 for parsing
		assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
		Files.delete(path);
	}

	@Test
	void parsePkcs1EcKeyFile() throws IOException {
		Path path = this.fileWriter.writeFile("key.pem", PemFileWriter.PRIVATE_EC_KEY);
		PrivateKey privateKey = PrivateKeyParser.parse(path);
		assertThat(privateKey).isNotNull();
		// keys in PKCS#1 format are converted to PKCS#8 for parsing
		assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
		Files.delete(path);
	}

	@Test
	void parseWithNonKeyFileWillThrowException() throws IOException {
		Path path = this.fileWriter.writeFile("text.pem", "plain text");
		assertThatIllegalStateException().isThrownBy(() -> PrivateKeyParser.parse(path))
			.withMessageContaining(path.toString());
		Files.delete(path);
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
