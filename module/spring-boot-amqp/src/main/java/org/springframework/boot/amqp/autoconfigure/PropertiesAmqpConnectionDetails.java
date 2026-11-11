/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.amqp.autoconfigure;

import org.jspecify.annotations.Nullable;

/**
 * Adapts {@link AmqpProperties} to {@link AmqpConnectionDetails}.
 *
 * @author Stephane Nicoll
 */
class PropertiesAmqpConnectionDetails implements AmqpConnectionDetails {

	private final AmqpProperties properties;

	PropertiesAmqpConnectionDetails(AmqpProperties properties) {
		this.properties = properties;
	}

	@Override
	public Address getAddress() {
		return new Address(this.properties.getHost(), this.properties.getPort());
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
