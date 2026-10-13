/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.rabbitmq.autoconfigure;

import java.time.Duration;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.boot.context.properties.PropertyMapper;

/**
 * Configures Rabbit {@link CachingConnectionFactory} with sensible defaults tuned using
 * configuration properties.
 * <p>
 * Can be injected into application code and used to define a custom
 * {@code CachingConnectionFactory} whose configuration is based upon that produced by
 * auto-configuration.
 *
 * @author Chris Bono
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 4.2.0
 */
public class CachingConnectionFactoryConfigurer extends AbstractConnectionFactoryConfigurer<CachingConnectionFactory> {

	/**
	 * Creates a new configurer that will configure the connection factory using the given
	 * {@code properties}.
	 * @param properties the properties to use to configure the connection factory
	 */
	public CachingConnectionFactoryConfigurer(RabbitProperties properties) {
		this(properties, new PropertiesRabbitConnectionDetails(properties, null));
	}

	/**
	 * Creates a new configurer that will configure the connection factory using the given
	 * {@code properties} and {@code connectionDetails}, with the latter taking priority.
	 * @param properties the properties to use to configure the connection factory
	 * @param connectionDetails the connection details to use to configure the connection
	 * factory
	 */
	public CachingConnectionFactoryConfigurer(RabbitProperties properties, RabbitConnectionDetails connectionDetails) {
		super(properties, connectionDetails);
	}

	@Override
	public void configure(CachingConnectionFactory connectionFactory, RabbitProperties rabbitProperties) {
		PropertyMapper map = PropertyMapper.get();
		map.from(rabbitProperties::isPublisherReturns).to(connectionFactory::setPublisherReturns);
		map.from(rabbitProperties::getPublisherConfirmType).to(connectionFactory::setPublisherConfirmType);
		RabbitProperties.Cache.Channel channel = rabbitProperties.getCache().getChannel();
		map.from(channel::getSize).to(connectionFactory::setChannelCacheSize);
		map.from(channel::getCheckoutTimeout).as(Duration::toMillis).to(connectionFactory::setChannelCheckoutTimeout);
		RabbitProperties.Cache.Connection connection = rabbitProperties.getCache().getConnection();
		map.from(connection::getMode).to(connectionFactory::setCacheMode);
		map.from(connection::getSize).to(connectionFactory::setConnectionCacheSize);
	}

}
