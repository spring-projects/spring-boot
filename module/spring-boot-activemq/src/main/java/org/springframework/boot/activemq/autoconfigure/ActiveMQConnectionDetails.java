/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.activemq.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;

/**
 * Details required to establish a connection to an ActiveMQ service.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @since 4.0.0
 */
public interface ActiveMQConnectionDetails extends ConnectionDetails {

	/**
	 * Broker URL to use.
	 * @return the url of the broker
	 */
	String getBrokerUrl();

	/**
	 * Login user to authenticate to the broker.
	 * @return the login user to authenticate to the broker or {@code null}
	 */
	@Nullable String getUser();

	/**
	 * Login to authenticate against the broker.
	 * @return the login to authenticate against the broker or {@code null}
	 */
	@Nullable String getPassword();

}
