/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.pulsar.autoconfigure;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;

/**
 * Details required to establish a connection to a Pulsar service.
 *
 * @author Chris Bono
 * @since 4.0.0
 */
public interface PulsarConnectionDetails extends ConnectionDetails {

	/**
	 * URL used to connect to the broker.
	 * @return the service URL
	 */
	String getBrokerUrl();

	/**
	 * URL user to connect to the admin endpoint.
	 * @return the admin URL
	 */
	String getAdminUrl();

}
