/*
 * Copyright 2012-present the original author or authors.
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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.info.SslInfo;
import org.springframework.boot.info.SslInfo.BundleInfo;
import org.springframework.boot.info.SslInfo.CertificateChainInfo;
import org.springframework.boot.info.SslInfo.CertificateInfo;
import org.springframework.util.Assert;

/**
 * {@link HealthIndicator} that checks the certificates the application uses and reports
 * {@link Status#OUT_OF_SERVICE} when a certificate is invalid.
 *
 * @author Jonatan Ivanov
 * @author Young Jae You
 * @since 3.4.0
 */
public class SslHealthIndicator extends AbstractHealthIndicator {

	private final SslInfo sslInfo;

	private final Duration expiryThreshold;

	public SslHealthIndicator(SslInfo sslInfo, Duration expiryThreshold) {
		super("SSL health check failed");
		Assert.notNull(sslInfo, "'sslInfo' must not be null");
		this.sslInfo = sslInfo;
		this.expiryThreshold = expiryThreshold;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		List<CertificateChainInfo> validCertificateChains = new ArrayList<>();
		List<CertificateChainInfo> invalidCertificateChains = new ArrayList<>();
		List<CertificateChainInfo> expiringCerificateChains = new ArrayList<>();
		for (BundleInfo bundle : this.sslInfo.getBundles()) {
			for (CertificateChainInfo certificateChain : bundle.getCertificateChains()) {
				if (containsOnlyValidCertificates(certificateChain)) {
					validCertificateChains.add(certificateChain);
					if (containsExpiringCertificate(certificateChain)) {
						expiringCerificateChains.add(certificateChain);
					}
				}
				else if (containsInvalidCertificate(certificateChain)) {
					invalidCertificateChains.add(certificateChain);
				}
			}
		}
		builder.status((invalidCertificateChains.isEmpty()) ? Status.UP : Status.OUT_OF_SERVICE);
		builder.withDetail("expiringChains", expiringCerificateChains);
		builder.withDetail("invalidChains", invalidCertificateChains);
		builder.withDetail("validChains", validCertificateChains);
	}

	private boolean containsOnlyValidCertificates(CertificateChainInfo certificateChain) {
		return validatableCertificates(certificateChain).allMatch(this::isValidCertificate);
	}

	private boolean containsInvalidCertificate(CertificateChainInfo certificateChain) {
		return validatableCertificates(certificateChain).anyMatch(this::isNotValidCertificate);
	}

	private boolean containsExpiringCertificate(CertificateChainInfo certificateChain) {
		return validatableCertificates(certificateChain).anyMatch(this::isExpiringCertificate);
	}

	private Stream<CertificateInfo> validatableCertificates(CertificateChainInfo certificateChain) {
		return certificateChain.getCertificates().stream().filter((certificate) -> certificate.getValidity() != null);
	}

	private boolean isValidCertificate(CertificateInfo certificate) {
		return certificate.getValidity().getStatus().isValid();
	}

	private boolean isNotValidCertificate(CertificateInfo certificate) {
		return !isValidCertificate(certificate);
	}

	private boolean isExpiringCertificate(CertificateInfo certificate) {
		return Instant.now().plus(this.expiryThreshold).isAfter(certificate.getValidityEnds());
	}

}
