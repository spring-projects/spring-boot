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

import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.sql.Date;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.boot.ssl.pem.PemDirectorySslStoreBundle.Certificate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link CertificateSelectors.MaximumNotAfterCertificateSelector}.
 *
 * @author Moritz Halbritter
 */
class MaximumNotAfterCertificateSelectorTests {

	private static final Instant NOW = Instant.parse("2000-01-01T00:00:00Z");

	private final CertificateSelectors.MaximumNotAfterCertificateSelector selector = new CertificateSelectors.MaximumNotAfterCertificateSelector(
			Clock.fixed(NOW, ZoneId.of("UTC")));

	@Test
	void shouldSelectCertificateWithMaximumNotAfter() {
		// Valid until 10s
		Certificate cert1 = new Certificate(Path.of("certificate-1"),
				createCertificate(NOW.minusSeconds(10), NOW.plusSeconds(10)));
		// Not valid, starts in the future
		Certificate cert2 = new Certificate(Path.of("certificate-2"),
				createCertificate(NOW.plusSeconds(1), NOW.plusSeconds(15)));
		// Not valid, expired
		Certificate cert3 = new Certificate(Path.of("certificate-3"),
				createCertificate(NOW.minusSeconds(10), NOW.minusSeconds(1)));
		// Valid until 20s
		Certificate cert4 = new Certificate(Path.of("certificate-4"),
				createCertificate(NOW.minusSeconds(10), NOW.plusSeconds(20)));
		List<Certificate> candidates = List.of(cert1, cert2, cert3, cert4);
		Certificate selected = this.selector.select(candidates);
		assertThat(selected).isEqualTo(cert4);
	}

	private X509Certificate createCertificate(Instant notBefore, Instant notAfter) {
		X509Certificate certificate = Mockito.mock(X509Certificate.class);
		given(certificate.getNotBefore()).willReturn(Date.from(notBefore));
		given(certificate.getNotAfter()).willReturn(Date.from(notAfter));
		return certificate;
	}

}
