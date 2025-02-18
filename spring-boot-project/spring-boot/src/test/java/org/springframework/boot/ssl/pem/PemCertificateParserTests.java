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
import java.security.cert.X509Certificate;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PemCertificateParser}.
 *
 * @author Scott Frederick
 */
class PemCertificateParserTests {

	@Test
	void parseCertificate() throws Exception {
		List<X509Certificate> certificates = PemCertificateParser.parse(read("test-cert.pem"));
		assertThat(certificates).isNotNull();
		assertThat(certificates).hasSize(1);
		assertThat(certificates.get(0).getType()).isEqualTo("X.509");
	}

	@Test
	void parseCertificateChain() throws Exception {
		List<X509Certificate> certificates = PemCertificateParser.parse(read("test-cert-chain.pem"));
		assertThat(certificates).isNotNull();
		assertThat(certificates).hasSize(2);
		assertThat(certificates.get(0).getType()).isEqualTo("X.509");
		assertThat(certificates.get(1).getType()).isEqualTo("X.509");
	}

	private String read(String path) throws IOException {
		return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
	}

}
