/*
 * Copyright 2012-2023 the original author or authors.
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

import java.util.stream.Collectors;

import org.springframework.amqp.rabbit.connection.AbstractConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionNameStrategy;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.util.Assert;

/**
 * Configures {@link AbstractConnectionFactory Rabbit ConnectionFactory} with sensible
 * defaults.
 *
 * @param <T> the connection factory type.
 * @author Chris Bono
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 2.6.0
 */
public abstract class AbstractConnectionFactoryConfigurer<T extends AbstractConnectionFactory> {

	private final RabbitProperties rabbitProperties;

	private ConnectionNameStrategy connectionNameStrategy;

	private final RabbitConnectionDetails connectionDetails;

	/**
	 * Creates a new configurer that will configure the connection factory using the given
	 * {@code properties}.
	 * @param properties the properties to use to configure the connection factory
	 */
	protected AbstractConnectionFactoryConfigurer(RabbitProperties properties) {
		this(properties, new PropertiesRabbitConnectionDetails(properties));
	}

	/**
	 * Creates a new configurer that will configure the connection factory using the given
	 * {@code properties} and {@code connectionDetails}, with the latter taking priority.
	 * @param properties the properties to use to configure the connection factory
	 * @param connectionDetails the connection details to use to configure the connection
	 * factory
	 * @since 3.1.0
	 */
	protected AbstractConnectionFactoryConfigurer(RabbitProperties properties,
			RabbitConnectionDetails connectionDetails) {
		Assert.notNull(properties, "Properties must not be null");
		Assert.notNull(connectionDetails, "ConnectionDetails must not be null");
		this.rabbitProperties = properties;
		this.connectionDetails = connectionDetails;
	}

	protected final ConnectionNameStrategy getConnectionNameStrategy() {
		return this.connectionNameStrategy;
	}

	public final void setConnectionNameStrategy(ConnectionNameStrategy connectionNameStrategy) {
		this.connectionNameStrategy = connectionNameStrategy;
	}

	/**
	 * Configures the given {@code connectionFactory} with sensible defaults.
	 * @param connectionFactory connection factory to configure
	 */
	public final void configure(T connectionFactory) {
		Assert.notNull(connectionFactory, "ConnectionFactory must not be null");
		PropertyMapper map = PropertyMapper.get();
		String addresses = this.connectionDetails.getAddresses()
			.stream()
			.map((address) -> address.host() + ":" + address.port())
			.collect(Collectors.joining(","));
		map.from(addresses).to(connectionFactory::setAddresses);
		map.from(this.rabbitProperties::getAddressShuffleMode)
			.whenNonNull()
			.to(connectionFactory::setAddressShuffleMode);
		map.from(this.connectionNameStrategy).whenNonNull().to(connectionFactory::setConnectionNameStrategy);
		configure(connectionFactory, this.rabbitProperties);
	}

	/**
	 * Configures the given {@code connectionFactory} using the given
	 * {@code rabbitProperties}.
	 * @param connectionFactory connection factory to configure
	 * @param rabbitProperties properties to use for the configuration
	 */
	protected abstract void configure(T connectionFactory, RabbitProperties rabbitProperties);

}
