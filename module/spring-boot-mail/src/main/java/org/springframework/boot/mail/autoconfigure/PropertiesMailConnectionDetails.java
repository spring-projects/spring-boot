/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.mail.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Adapts {@link MailProperties} to {@link MailConnectionDetails}.
 *
 * @author Van Vo
 */
class PropertiesMailConnectionDetails implements MailConnectionDetails {

	private final MailProperties properties;

	PropertiesMailConnectionDetails(MailProperties properties) {
		this.properties = properties;
	}

	@Override
	public @Nullable String getHost() {
		return this.properties.getHost();
	}

	@Override
	public int getPort() {
		Integer port = this.properties.getPort();
		return (port != null) ? port : JavaMailSenderImpl.DEFAULT_PORT;
	}

	@Override
	public @Nullable String getUsername() {
		return this.properties.getUsername();
	}

	@Override
	public @Nullable String getPassword() {
		return this.properties.getPassword();
	}

}
