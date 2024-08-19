/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.actuate.ssl;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.info.SslInfo;
import org.springframework.boot.info.SslInfo.BundleInfo;
import org.springframework.boot.info.SslInfo.CertificateChainInfo;
import org.springframework.boot.info.SslInfo.CertificateInfo;
import org.springframework.boot.info.SslInfo.CertificateValidityInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SslHealthIndicator}.
 *
 * @author Jonatan Ivanov
 */
class SslHealthIndicatorTests {

	private HealthIndicator healthIndicator;

	private CertificateValidityInfo validity;

	@BeforeEach
	void setUp() {
		SslInfo sslInfo = mock(SslInfo.class);
		BundleInfo bundle = mock(BundleInfo.class);
		CertificateChainInfo certificateChain = mock(CertificateChainInfo.class);
		CertificateInfo certificateInfo = mock(CertificateInfo.class);
		this.healthIndicator = new SslHealthIndicator(sslInfo);
		this.validity = mock(CertificateValidityInfo.class);
		given(sslInfo.getBundles()).willReturn(List.of(bundle));
		given(bundle.getCertificateChains()).willReturn(List.of(certificateChain));
		given(certificateChain.getCertificates()).willReturn(List.of(certificateInfo));
		given(certificateInfo.getValidity()).willReturn(this.validity);
	}

	@Test
	void shouldBeUpIfNoSslIssuesDetected() {
		given(this.validity.getStatus()).willReturn(CertificateValidityInfo.Status.VALID);
		Health health = this.healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertDetailsKeys(health);
		List<CertificateChainInfo> validChains = getValidChains(health);
		assertThat(validChains).hasSize(1);
		assertThat(validChains.get(0)).isInstanceOf(CertificateChainInfo.class);
		List<CertificateChainInfo> invalidChains = getInvalidChains(health);
		assertThat(invalidChains).isEmpty();
	}

	@Test
	void shouldBeOutOfServiceIfACertificateIsExpired() {
		given(this.validity.getStatus()).willReturn(CertificateValidityInfo.Status.EXPIRED);
		Health health = this.healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
		assertDetailsKeys(health);
		List<CertificateChainInfo> validChains = getValidChains(health);
		assertThat(validChains).isEmpty();
		List<CertificateChainInfo> invalidChains = getInvalidChains(health);
		assertThat(invalidChains).hasSize(1);
		assertThat(invalidChains.get(0)).isInstanceOf(CertificateChainInfo.class);
	}

	@Test
	void shouldBeOutOfServiceIfACertificateIsNotYetValid() {
		given(this.validity.getStatus()).willReturn(CertificateValidityInfo.Status.NOT_YET_VALID);
		Health health = this.healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
		assertDetailsKeys(health);
		List<CertificateChainInfo> validChains = getValidChains(health);
		assertThat(validChains).isEmpty();
		List<CertificateChainInfo> invalidChains = getInvalidChains(health);
		assertThat(invalidChains).hasSize(1);
		assertThat(invalidChains.get(0)).isInstanceOf(CertificateChainInfo.class);

	}

	@Test
	void shouldReportWarningIfACertificateWillExpireSoon() {
		given(this.validity.getStatus()).willReturn(CertificateValidityInfo.Status.WILL_EXPIRE_SOON);
		Health health = this.healthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertDetailsKeys(health);
		List<CertificateChainInfo> validChains = getValidChains(health);
		assertThat(validChains).hasSize(1);
		assertThat(validChains.get(0)).isInstanceOf(CertificateChainInfo.class);
		List<CertificateChainInfo> invalidChains = getInvalidChains(health);
		assertThat(invalidChains).isEmpty();
	}

	private static void assertDetailsKeys(Health health) {
		assertThat(health.getDetails()).containsOnlyKeys("validChains", "invalidChains");
	}

	@SuppressWarnings("unchecked")
	private static List<CertificateChainInfo> getInvalidChains(Health health) {
		return (List<CertificateChainInfo>) health.getDetails().get("invalidChains");
	}

	@SuppressWarnings("unchecked")
	private static List<CertificateChainInfo> getValidChains(Health health) {
		return (List<CertificateChainInfo>) health.getDetails().get("validChains");
	}

}
