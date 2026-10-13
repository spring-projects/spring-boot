/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.rabbitmq.autoconfigure;

import org.springframework.amqp.rabbit.config.DirectRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.context.properties.PropertyMapper;

/**
 * Configure {@link DirectRabbitListenerContainerFactory} with sensible defaults tuned
 * using configuration properties.
 * <p>
 * Can be injected into application code and used to define a custom
 * {@code DirectRabbitListenerContainerFactory} whose configuration is based upon that
 * produced by auto-configuration.
 *
 * @author Gary Russell
 * @author Stephane Nicoll
 * @since 4.2.0
 */
public final class DirectRabbitListenerContainerFactoryConfigurer
		extends AbstractRabbitListenerContainerFactoryConfigurer<DirectRabbitListenerContainerFactory> {

	/**
	 * Creates a new configurer that will use the given {@code rabbitProperties}.
	 * @param rabbitProperties properties to use
	 */
	public DirectRabbitListenerContainerFactoryConfigurer(RabbitProperties rabbitProperties) {
		super(rabbitProperties);
	}

	@Override
	public void configure(DirectRabbitListenerContainerFactory factory, ConnectionFactory connectionFactory) {
		PropertyMapper map = PropertyMapper.get();
		RabbitProperties.DirectContainer config = getRabbitProperties().getListener().getDirect();
		configure(factory, connectionFactory, config);
		map.from(config::getConsumersPerQueue).to(factory::setConsumersPerQueue);
	}

}
