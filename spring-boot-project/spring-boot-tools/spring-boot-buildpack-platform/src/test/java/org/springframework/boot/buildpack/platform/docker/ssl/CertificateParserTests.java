/*
 * Copyright 2012-2020 the original author or authors.
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
import java.security.cert.X509Certificate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link CertificateParser}.
 *
 * @author Scott Frederick
 */
class CertificateParserTests {

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
	void parseCertificates() throws IOException {
		Path caPath = this.fileWriter.writeFile("ca.pem", PemFileWriter.CA_CERTIFICATE);
		Path certPath = this.fileWriter.writeFile("cert.pem", PemFileWriter.CERTIFICATE);
		X509Certificate[] certificates = CertificateParser.parse(caPath, certPath);
		assertThat(certificates).isNotNull();
		assertThat(certificates.length).isEqualTo(2);
		assertThat(certificates[0].getType()).isEqualTo("X.509");
		assertThat(certificates[1].getType()).isEqualTo("X.509");
	}

	@Test
	void parseCertificateChain() throws IOException {
		Path path = this.fileWriter.writeFile("ca.pem", PemFileWriter.CA_CERTIFICATE, PemFileWriter.CERTIFICATE);
		X509Certificate[] certificates = CertificateParser.parse(path);
		assertThat(certificates).isNotNull();
		assertThat(certificates.length).isEqualTo(2);
		assertThat(certificates[0].getType()).isEqualTo("X.509");
		assertThat(certificates[1].getType()).isEqualTo("X.509");
	}

	@Test
	void parseWithInvalidPathWillThrowException() throws URISyntaxException {
		Path path = Paths.get(new URI("file:///bad/path/cert.pem"));
		assertThatIllegalStateException().isThrownBy(() -> CertificateParser.parse(path))
				.withMessageContaining(path.toString());
	}

}
