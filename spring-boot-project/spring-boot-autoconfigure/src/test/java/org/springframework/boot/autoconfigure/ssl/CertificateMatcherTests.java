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
