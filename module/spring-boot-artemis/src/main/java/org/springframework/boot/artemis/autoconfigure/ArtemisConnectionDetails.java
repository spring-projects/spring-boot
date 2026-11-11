/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.artemis.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;

/**
 * Details required to establish a connection to an Artemis service.
 *
 * @author Eddú Meléndez
 * @since 4.0.0
 */
public interface ArtemisConnectionDetails extends ConnectionDetails {

	/**
	 * Artemis deployment mode, auto-detected by default.
	 * @return the Artemis deployment mode, auto-detected by default
	 */
	@Nullable ArtemisMode getMode();

	/**
	 * Artemis broker url.
	 * @return the Artemis broker url
	 */
	@Nullable String getBrokerUrl();

	/**
	 * Login user of the broker.
	 * @return the login user of the broker
	 */
	@Nullable String getUser();

	/**
	 * Login password of the broker.
	 * @return the login password of the broker
	 */
	@Nullable String getPassword();

}
