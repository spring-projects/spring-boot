/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.rabbitmq.autoconfigure;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.context.properties.PropertyMapper;

/**
 * Configure {@link SimpleRabbitListenerContainerFactory} with sensible defaults tuned
 * using configuration properties.
 * <p>
 * Can be injected into application code and used to define a custom
 * {@code SimpleRabbitListenerContainerFactory} whose configuration is based upon that
 * produced by auto-configuration.
 *
 * @author Stephane Nicoll
 * @author Gary Russell
 * @since 4.2.0
 */
public final class SimpleRabbitListenerContainerFactoryConfigurer
		extends AbstractRabbitListenerContainerFactoryConfigurer<SimpleRabbitListenerContainerFactory> {

	/**
	 * Creates a new configurer that will use the given {@code rabbitProperties}.
	 * @param rabbitProperties properties to use
	 */
	public SimpleRabbitListenerContainerFactoryConfigurer(RabbitProperties rabbitProperties) {
		super(rabbitProperties);
	}

	@Override
	public void configure(SimpleRabbitListenerContainerFactory factory, ConnectionFactory connectionFactory) {
		PropertyMapper map = PropertyMapper.get();
		RabbitProperties.SimpleContainer config = getRabbitProperties().getListener().getSimple();
		configure(factory, connectionFactory, config);
		map.from(config::getConcurrency).to(factory::setConcurrentConsumers);
		map.from(config::getMaxConcurrency).to(factory::setMaxConcurrentConsumers);
		map.from(config::getBatchSize).to(factory::setBatchSize);
		map.from(config::isConsumerBatchEnabled).to(factory::setConsumerBatchEnabled);
	}

}
