/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.rabbitmq.autoconfigure;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.util.Assert;

/**
 * Details required to establish a connection to a RabbitMQ service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 4.2.0
 */
public interface RabbitConnectionDetails extends ConnectionDetails {

	/**
	 * Login user to authenticate to the broker.
	 * @return the login user to authenticate to the broker or {@code null}
	 */
	default @Nullable String getUsername() {
		return null;
	}

	/**
	 * Login to authenticate against the broker.
	 * @return the login to authenticate against the broker or {@code null}
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
	 * List of addresses to which the client should connect. Must return at least one
	 * address.
	 * @return the list of addresses to which the client should connect
	 */
	List<Address> getAddresses();

	/**
	 * Returns the first address.
	 * @return the first address
	 * @throws IllegalStateException if the address list is empty
	 */
	default Address getFirstAddress() {
		List<Address> addresses = getAddresses();
		Assert.state(!addresses.isEmpty(), "Address list is empty");
		return addresses.get(0);
	}

	/**
	 * SSL bundle to use.
	 * @return the SSL bundle to use
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
