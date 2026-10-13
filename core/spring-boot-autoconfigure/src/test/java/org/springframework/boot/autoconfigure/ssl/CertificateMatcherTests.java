/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.ssl;

import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CertificateMatcher}.
 *
 * @author Moritz Halbritter
 * @author Phillip Webb
 */
class CertificateMatcherTests {

	@CertificateMatchingTest
	void matchesWhenMatchReturnsTrue(CertificateMatchingTestSource source) {
		CertificateMatcher matcher = new CertificateMatcher(source.privateKey());
		assertThat(matcher.matches(source.matchingCertificate())).isTrue();
	}

	@CertificateMatchingTest
	void matchesWhenNoMatchReturnsFalse(CertificateMatchingTestSource source) {
		CertificateMatcher matcher = new CertificateMatcher(source.privateKey());
		for (Certificate nonMatchingCertificate : source.nonMatchingCertificates()) {
			assertThat(matcher.matches(nonMatchingCertificate)).isFalse();
		}
	}

	@CertificateMatchingTest
	void matchesAnyWhenNoneMatchReturnsFalse(CertificateMatchingTestSource source) {
		CertificateMatcher matcher = new CertificateMatcher(source.privateKey());
		assertThat(matcher.matchesAny(source.nonMatchingCertificates())).isFalse();
	}

	@CertificateMatchingTest
	void matchesAnyWhenOneMatchesReturnsTrue(CertificateMatchingTestSource source) {
		CertificateMatcher matcher = new CertificateMatcher(source.privateKey());
		List<Certificate> certificates = new ArrayList<>(source.nonMatchingCertificates());
		certificates.add(source.matchingCertificate());
		assertThat(matcher.matchesAny(certificates)).isTrue();
	}

}
