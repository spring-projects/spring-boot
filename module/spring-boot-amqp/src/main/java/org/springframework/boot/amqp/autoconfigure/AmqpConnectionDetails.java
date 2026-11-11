/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.amqp.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;

/**
 * Details required to establish a connection to an AMQP broker.
 *
 * @author Stephane Nicoll
 * @since 4.2.0
 */
public interface AmqpConnectionDetails extends ConnectionDetails {

	/**
	 * Return the {@link Address} of the broker.
	 * @return the address
	 */
	Address getAddress();

	/**
	 * Login user to authenticate to the broker.
	 * @return the login user to authenticate to the broker or {@code null}
	 */
	default @Nullable String getUsername() {
		return null;
	}

	/**
	 * Password used to authenticate to the broker.
	 * @return the password to authenticate to the broker or {@code null}
	 */
	default @Nullable String getPassword() {
		return null;
	}

	/**
	 * An AMQP broker address.
	 *
	 * @param host the host
	 * @param port the port
	 */
	record Address(String host, int port) {
	}

}
