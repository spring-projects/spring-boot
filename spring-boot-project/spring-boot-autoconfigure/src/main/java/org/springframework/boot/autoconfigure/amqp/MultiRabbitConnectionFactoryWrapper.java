/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.amqp;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.util.Assert;

/**
 * A wrapper for RabbitMQ structures to offer an easy way to integrate with MultiRabbit.
 * It's backed by a {@link HashMap} that holds all non-default structures, but does not
 * allow null keys since there is a default field holding .
 *
 * @author Wander Costa
 * @since 2.4
 */
public class MultiRabbitConnectionFactoryWrapper {

	private final Map<Object, ConnectionFactory> connectionFactories = new HashMap<>();

	private final Map<Object, ConnectionFactory> unmodifiableMapReference = Collections
			.unmodifiableMap(this.connectionFactories);

	private ConnectionFactory defaultConnectionFactory;

	public void setDefaultConnectionFactory(final ConnectionFactory connectionFactory) {
		this.defaultConnectionFactory = connectionFactory;
	}

	ConnectionFactory getDefaultConnectionFactory() {
		return this.defaultConnectionFactory;
	}

	public void addConnectionFactory(final String key, final ConnectionFactory connectionFactory) {
		Assert.hasText(key, "Key may not be null or empty");
		this.connectionFactories.put(key, connectionFactory);
	}

	Map<Object, ConnectionFactory> getConnectionFactories() {
		return this.unmodifiableMapReference;
	}

}
