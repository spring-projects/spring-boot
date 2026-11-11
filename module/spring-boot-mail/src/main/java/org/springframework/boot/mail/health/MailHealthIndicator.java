/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.mail.health;

import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.util.StringUtils;

/**
 * {@link HealthIndicator} for configured smtp server(s).
 *
 * @author Johannes Edmeier
 * @author Scott Frederick
 * @since 4.0.0
 */
public class MailHealthIndicator extends AbstractHealthIndicator {

	private final JavaMailSenderImpl mailSender;

	public MailHealthIndicator(JavaMailSenderImpl mailSender) {
		super("Mail health check failed");
		this.mailSender = mailSender;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		String host = this.mailSender.getHost();
		int port = this.mailSender.getPort();
		StringBuilder location = new StringBuilder((host != null) ? host : "");
		if (port != JavaMailSenderImpl.DEFAULT_PORT) {
			location.append(":").append(port);
		}
		if (StringUtils.hasLength(location)) {
			builder.withDetail("location", location.toString());
		}
		this.mailSender.testConnection();
		builder.up();
	}

}
