/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.autoconfigure.application;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.health.application.SslHealthIndicator;

/**
 * External configuration properties for {@link SslHealthIndicator}.
 *
 * @author Jonatan Ivanov
 * @since 4.0.0
 */
@ConfigurationProperties("management.health.ssl")
public class SslHealthIndicatorProperties {

	/**
	 * If an SSL Certificate will be invalid within the time span defined by this
	 * threshold, it should trigger a warning.
	 */
	private Duration certificateValidityWarningThreshold = Duration.ofDays(14);

	public Duration getCertificateValidityWarningThreshold() {
		return this.certificateValidityWarningThreshold;
	}

	public void setCertificateValidityWarningThreshold(Duration certificateValidityWarningThreshold) {
		this.certificateValidityWarningThreshold = certificateValidityWarningThreshold;
	}

}
