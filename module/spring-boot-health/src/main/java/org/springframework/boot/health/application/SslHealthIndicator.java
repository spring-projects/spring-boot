/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.application;

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
import org.springframework.boot.info.SslInfo.CertificateValidityInfo;
import org.springframework.util.Assert;

/**
 * {@link HealthIndicator} that checks the certificates the application uses and reports
 * {@link Status#OUT_OF_SERVICE} when a certificate is invalid.
 *
 * @author Jonatan Ivanov
 * @author Young Jae You
 * @since 4.0.0
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
		List<CertificateChainInfo> expiringCertificateChains = new ArrayList<>();
		for (BundleInfo bundle : this.sslInfo.getBundles()) {
			for (CertificateChainInfo certificateChain : bundle.getCertificateChains()) {
				if (containsOnlyValidCertificates(certificateChain)) {
					validCertificateChains.add(certificateChain);
					if (containsExpiringCertificate(certificateChain)) {
						expiringCertificateChains.add(certificateChain);
					}
				}
				else if (containsInvalidCertificate(certificateChain)) {
					invalidCertificateChains.add(certificateChain);
				}
			}
		}
		builder.status((invalidCertificateChains.isEmpty()) ? Status.UP : Status.OUT_OF_SERVICE);
		builder.withDetail("expiringChains", expiringCertificateChains);
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
		CertificateValidityInfo validity = certificate.getValidity();
		Assert.state(validity != null, "'validity' must not be null");
		return validity.getStatus().isValid();
	}

	private boolean isNotValidCertificate(CertificateInfo certificate) {
		return !isValidCertificate(certificate);
	}

	private boolean isExpiringCertificate(CertificateInfo certificate) {
		Instant validityEnds = certificate.getValidityEnds();
		Assert.state(validityEnds != null, "'validityEnds' must not be null");
		return Instant.now().plus(this.expiryThreshold).isAfter(validityEnds);
	}

}
