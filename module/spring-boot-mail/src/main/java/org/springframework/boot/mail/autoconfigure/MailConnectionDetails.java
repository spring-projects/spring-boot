/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.mail.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;

/**
 * Details required to connect to a mail server.
 *
 * @author Van Vo
 * @since 4.2.0
 */
public interface MailConnectionDetails extends ConnectionDetails {

	/**
	 * Return the host of the mail server.
	 * @return the host, or {@code null} to use the mail session's default
	 */
	@Nullable String getHost();

	/**
	 * Return the port of the mail server.
	 * @return the port, or {@code -1} to use the protocol's default
	 */
	int getPort();

	/**
	 * Return the username for the mail server.
	 * @return the username, or {@code null} if none
	 */
	default @Nullable String getUsername() {
		return null;
	}

	/**
	 * Return the password for the mail server.
	 * @return the password, or {@code null} if none
	 */
	default @Nullable String getPassword() {
		return null;
	}

}
