/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.rabbitmq.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.ssl.SslBundle;

/**
 * Details required to establish a connection to a RabbitMQ Stream service.
 *
 * @author Eddú Meléndez
 * @author Jay Choi
 * @since 4.2.0
 */
public interface RabbitStreamConnectionDetails extends ConnectionDetails {

	/**
	 * Rabbit Stream server host.
	 * @return the Rabbit Stream server host
	 */
	String getHost();

	/**
	 * Rabbit Stream server port.
	 * @return the Rabbit Stream server port
	 */
	int getPort();

	/**
	 * Username for authentication.
	 * @return the username for authentication or {@code null}
	 */
	default @Nullable String getUsername() {
		return null;
	}

	/**
	 * Password for authentication.
	 * @return the password for authentication or {@code null}
	 */
	default @Nullable String getPassword() {
		return null;
	}

	/**
	 * Virtual host to use when connecting to the broker.
	 * @return the virtual host to use when connecting to the broker or {@code null}
	 */
	default @Nullable String getVirtualHost() {
		return null;
	}

	/**
	 * SSL bundle to use.
	 * @return the SSL bundle to use or {@code null}
	 */
	default @Nullable SslBundle getSslBundle() {
		return null;
	}

}
