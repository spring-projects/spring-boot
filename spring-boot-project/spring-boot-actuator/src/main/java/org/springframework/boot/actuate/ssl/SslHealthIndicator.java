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
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.info.SslInfo;
import org.springframework.boot.info.SslInfo.CertificateInfo;
import org.springframework.boot.info.SslInfo.CertificateInfo.Validity;

/**
 * {@link HealthIndicator} that checks the certificates the application uses and reports
 * {@link Status#OUT_OF_SERVICE} when a certificate is invalid or "WILL_EXPIRE_SOON" if it
 * will expire within the configurable threshold.
 *
 * @author Jonatan Ivanov
 * @since 3.4.0
 */
public class SslHealthIndicator extends AbstractHealthIndicator {

	private static final Status WILL_EXPIRE_SOON_STATUS = new Status(Validity.Status.WILL_EXPIRE_SOON.name(),
			"One of the certificates will expire within the defined threshold.");

	private final SslInfo sslInfo;

	public SslHealthIndicator(SslInfo sslInfo) {
		this.sslInfo = sslInfo;
	}

	@Override
	protected void doHealthCheck(Builder builder) throws Exception {
		List<CertificateInfo> notValidCertificates = this.sslInfo.getBundles()
			.stream()
			.flatMap((bundle) -> bundle.getCertificateChains().stream())
			.flatMap((certificateChain) -> certificateChain.getCertificates().stream())
			.filter((certificate) -> certificate.getValidity() != null)
			.filter((certificate) -> certificate.getValidity().getStatus() != Validity.Status.VALID)
			.toList();

		if (notValidCertificates.isEmpty()) {
			builder.status(Status.UP);
		}
		else {
			Set<Validity.Status> statuses = notValidCertificates.stream()
				.map((certificate) -> certificate.getValidity().getStatus())
				.collect(Collectors.toUnmodifiableSet());
			if (statuses.contains(Validity.Status.EXPIRED) || statuses.contains(Validity.Status.NOT_YET_VALID)) {
				builder.status(Status.OUT_OF_SERVICE);
			}
			else if (statuses.contains(Validity.Status.WILL_EXPIRE_SOON)) {
				// TODO: Should we introduce Status.WARNING
				// (returns 200 but indicates that something is not right)?
				builder.status(WILL_EXPIRE_SOON_STATUS);
			}
			else {
				builder.status(Status.OUT_OF_SERVICE);
			}
			builder.withDetail("certificates", notValidCertificates);
		}
	}

}
