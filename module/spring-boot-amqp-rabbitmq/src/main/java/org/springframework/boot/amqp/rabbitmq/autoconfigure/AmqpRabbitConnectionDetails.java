/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.amqp.rabbitmq.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.ssl.SslBundle;

/**
 * Details required to establish a connection to a RabbitMQ AMQP service.
 *
 * @author Eddú Meléndez
 * @since 4.2.0
 */
public interface AmqpRabbitConnectionDetails extends ConnectionDetails {

	/**
	 * Return the address of the broker.
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

	/**
	 * A RabbitMQ address.
	 *
	 * @param host the host
	 * @param port the port
	 */
	record Address(String host, int port) {
	}

}
