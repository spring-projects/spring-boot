/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.web.server;

import java.security.cert.X509Certificate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link CertificateParser}.
 *
 * @author Scott Frederick
 */
class CertificateParserTests {

	@Test
	void parseCertificate() {
		X509Certificate[] certificates = CertificateParser.parse("classpath:test-cert.pem");
		assertThat(certificates).isNotNull();
		assertThat(certificates.length).isEqualTo(1);
		assertThat(certificates[0].getType()).isEqualTo("X.509");
	}

	@Test
	void parseCertificateChain() {
		X509Certificate[] certificates = CertificateParser.parse("classpath:test-cert-chain.pem");
		assertThat(certificates).isNotNull();
		assertThat(certificates.length).isEqualTo(2);
		assertThat(certificates[0].getType()).isEqualTo("X.509");
		assertThat(certificates[1].getType()).isEqualTo("X.509");
	}

	@Test
	void parseWithInvalidPathWillThrowException() {
		String path = "file:///bad/path/cert.pem";
		assertThatIllegalStateException().isThrownBy(() -> CertificateParser.parse("file:///bad/path/cert.pem"))
				.withMessageContaining(path);
	}

}
