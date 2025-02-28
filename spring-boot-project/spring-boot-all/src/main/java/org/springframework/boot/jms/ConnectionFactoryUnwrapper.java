/*
 * Copyright 2012-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.jms;

import jakarta.jms.ConnectionFactory;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;

import org.springframework.jms.connection.CachingConnectionFactory;

/**
 * Unwrap a {@link ConnectionFactory} that may have been wrapped to perform caching or
 * pooling.
 *
 * @author Stephane Nicoll
 * @since 3.4.0
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
	 * @since 3.4.1
	 */
	public static ConnectionFactory unwrapCaching(ConnectionFactory connectionFactory) {
		if (connectionFactory instanceof CachingConnectionFactory cachingConnectionFactory) {
			ConnectionFactory unwrapedConnectionFactory = cachingConnectionFactory.getTargetConnectionFactory();
			return (unwrapedConnectionFactory != null) ? unwrapCaching(unwrapedConnectionFactory) : connectionFactory;
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
	public static ConnectionFactory unwrap(ConnectionFactory connectionFactory) {
		if (connectionFactory instanceof CachingConnectionFactory cachingConnectionFactory) {
			return unwrap(cachingConnectionFactory.getTargetConnectionFactory());
		}
		ConnectionFactory unwrapedConnectionFactory = unwrapFromJmsPoolConnectionFactory(connectionFactory);
		return (unwrapedConnectionFactory != null) ? unwrap(unwrapedConnectionFactory) : connectionFactory;
	}

	private static ConnectionFactory unwrapFromJmsPoolConnectionFactory(ConnectionFactory connectionFactory) {
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
