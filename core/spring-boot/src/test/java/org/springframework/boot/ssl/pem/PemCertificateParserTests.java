/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.ssl.pem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.testsupport.classpath.resources.WithPackageResources;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PemCertificateParser}.
 *
 * @author Scott Frederick
 */
class PemCertificateParserTests {

	@Test
	@WithPackageResources("test-cert.pem")
	void parseCertificate() throws Exception {
		List<X509Certificate> certificates = PemCertificateParser.parse(read("test-cert.pem"));
		assertThat(certificates).isNotNull();
		assertThat(certificates).hasSize(1);
		assertThat(certificates.get(0).getType()).isEqualTo("X.509");
	}

	@Test
	@WithPackageResources("test-cert-chain.pem")
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
