/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.r2dbc;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.ConnectionFactoryProvider;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * Simple driver for testing.
 *
 * @author Mark Paluch
 * @author Andy Wilkinson
 */
public class SimpleConnectionFactoryProvider implements ConnectionFactoryProvider {

	@Override
	public ConnectionFactory create(ConnectionFactoryOptions connectionFactoryOptions) {
		return new SimpleTestConnectionFactory();
	}

	@Override
	public boolean supports(ConnectionFactoryOptions connectionFactoryOptions) {
		return connectionFactoryOptions.getRequiredValue(ConnectionFactoryOptions.DRIVER).equals("simple");
	}

	@Override
	public String getDriver() {
		return "simple";
	}

	public static class SimpleTestConnectionFactory implements ConnectionFactory {

		@Override
		public Publisher<? extends Connection> create() {
			return Mono.error(new UnsupportedOperationException());
		}

		@Override
		public ConnectionFactoryMetadata getMetadata() {
			return SimpleConnectionFactoryProvider.class::getName;
		}

	}

}
