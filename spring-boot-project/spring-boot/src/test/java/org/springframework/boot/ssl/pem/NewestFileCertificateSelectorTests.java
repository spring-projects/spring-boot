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
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.sql.Date;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import org.springframework.boot.ssl.pem.PemDirectorySslStoreBundle.Certificate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link CertificateSelectors.NewestFileCertificateSelector}.
 *
 * @author Moritz Halbritter
 */
class NewestFileCertificateSelectorTests {

	private static final Instant NOW = Instant.parse("2000-01-01T00:00:00Z");

	private final CertificateSelectors.NewestFileCertificateSelector selector = new CertificateSelectors.NewestFileCertificateSelector(
			Clock.fixed(NOW, ZoneId.of("UTC")));

	@Test
	void shouldSelectNewestFile(@TempDir Path tempDirectory) throws IOException, InterruptedException {
		// There's no portable way to set the file creation date, so we resort to sleeps
		// here
		Path cert1file = Files.createFile(tempDirectory.resolve("certificate-1"));
		Thread.sleep(10);
		Path cert2file = Files.createFile(tempDirectory.resolve("certificate-2"));
		Thread.sleep(10);
		Path cert3file = Files.createFile(tempDirectory.resolve("certificate-3"));
		Thread.sleep(10);
		Path cert4file = Files.createFile(tempDirectory.resolve("certificate-4"));
		// Valid
		Certificate cert1 = createCertificate(cert1file, NOW.minusSeconds(10), NOW.plusSeconds(10));
		// Not valid, starts in the future
		Certificate cert2 = createCertificate(cert2file, NOW.plusSeconds(1), NOW.plusSeconds(15));
		// Not valid, expired
		Certificate cert3 = createCertificate(cert3file, NOW.minusSeconds(1), NOW.minusSeconds(1));
		// Valid
		Certificate cert4 = createCertificate(cert4file, NOW.minusSeconds(1), NOW.plusSeconds(1));
		List<Certificate> candidates = List.of(cert1, cert2, cert3, cert4);
		Certificate selected = this.selector.select(candidates);
		assertThat(selected).isEqualTo(cert4);
	}

	private Certificate createCertificate(Path file, Instant notBefore, Instant notAfter) {
		X509Certificate certificate = Mockito.mock(X509Certificate.class);
		given(certificate.getNotBefore()).willReturn(Date.from(notBefore));
		given(certificate.getNotAfter()).willReturn(Date.from(notAfter));
		return new Certificate(file, certificate);
	}

}
