/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jms;

import jakarta.jms.ConnectionFactory;
import org.jspecify.annotations.Nullable;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;

import org.springframework.jms.connection.CachingConnectionFactory;

/**
 * Unwrap a {@link ConnectionFactory} that may have been wrapped to perform caching or
 * pooling.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
public final class ConnectionFactoryUnwrapper {

	private ConnectionFactoryUnwrapper() {
	}

	/**
	 * Return the native {@link ConnectionFactory} by unwrapping from a
	 * {@link CachingConnectionFactory}. Return the given {@link ConnectionFactory} if no
	 * {@link CachingConnectionFactory} wrapper has been detected.
	 * @param connectionFactory a connection factory
	 * @return the native connection factory that a {@link CachingConnectionFactory}
	 * wraps, if any
	 */
	public static ConnectionFactory unwrapCaching(ConnectionFactory connectionFactory) {
		if (connectionFactory instanceof CachingConnectionFactory cachingConnectionFactory) {
			ConnectionFactory unwrappedConnectionFactory = cachingConnectionFactory.getTargetConnectionFactory();
			return (unwrappedConnectionFactory != null) ? unwrapCaching(unwrappedConnectionFactory) : connectionFactory;
		}
		return connectionFactory;
	}

	/**
	 * Return the native {@link ConnectionFactory} by unwrapping it from a cache or pool
	 * connection factory. Return the given {@link ConnectionFactory} if no caching
	 * wrapper has been detected.
	 * @param connectionFactory a connection factory
	 * @return the native connection factory that it wraps, if any
	 */
	public static @Nullable ConnectionFactory unwrap(@Nullable ConnectionFactory connectionFactory) {
		if (connectionFactory instanceof CachingConnectionFactory cachingConnectionFactory) {
			return unwrap(cachingConnectionFactory.getTargetConnectionFactory());
		}
		ConnectionFactory unwrapedConnectionFactory = unwrapFromJmsPoolConnectionFactory(connectionFactory);
		return (unwrapedConnectionFactory != null) ? unwrap(unwrapedConnectionFactory) : connectionFactory;
	}

	private static @Nullable ConnectionFactory unwrapFromJmsPoolConnectionFactory(
			@Nullable ConnectionFactory connectionFactory) {
		try {
			if (connectionFactory instanceof JmsPoolConnectionFactory jmsPoolConnectionFactory) {
				return (ConnectionFactory) jmsPoolConnectionFactory.getConnectionFactory();
			}
		}
		catch (Throwable ex) {
			// ignore
		}
		return null;
	}

}
