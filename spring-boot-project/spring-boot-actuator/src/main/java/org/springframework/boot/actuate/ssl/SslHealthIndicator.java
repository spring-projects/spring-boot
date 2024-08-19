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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.info.SslInfo;
import org.springframework.boot.info.SslInfo.BundleInfo;
import org.springframework.boot.info.SslInfo.CertificateChainInfo;
import org.springframework.boot.info.SslInfo.CertificateInfo;

/**
 * {@link HealthIndicator} that checks the certificates the application uses and reports
 * {@link Status#OUT_OF_SERVICE} when a certificate is invalid.
 *
 * @author Jonatan Ivanov
 * @since 3.4.0
 */
public class SslHealthIndicator extends AbstractHealthIndicator {

	private final SslInfo sslInfo;

	public SslHealthIndicator(SslInfo sslInfo) {
		this.sslInfo = sslInfo;
	}

	@Override
	protected void doHealthCheck(Builder builder) throws Exception {
		List<CertificateChainInfo> validCertificateChains = new ArrayList<>();
		List<CertificateChainInfo> invalidCertificateChains = new ArrayList<>();
		for (BundleInfo bundle : this.sslInfo.getBundles()) {
			for (CertificateChainInfo certificateChain : bundle.getCertificateChains()) {
				if (containsOnlyValidCertificates(certificateChain)) {
					validCertificateChains.add(certificateChain);
				}
				else if (containsInvalidCertificate(certificateChain)) {
					invalidCertificateChains.add(certificateChain);
				}
			}
		}
		builder.status((invalidCertificateChains.isEmpty()) ? Status.UP : Status.OUT_OF_SERVICE);
		builder.withDetail("validChains", validCertificateChains);
		builder.withDetail("invalidChains", invalidCertificateChains);
	}

	private boolean containsOnlyValidCertificates(CertificateChainInfo certificateChain) {
		return validatableCertificates(certificateChain).allMatch(this::isValidCertificate);
	}

	private boolean containsInvalidCertificate(CertificateChainInfo certificateChain) {
		return validatableCertificates(certificateChain).anyMatch(this::isNotValidCertificate);
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

}
