/*
 * Copyright 2012-2022 the original author or authors.
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

import org.springframework.amqp.rabbit.config.DirectRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.context.properties.PropertyMapper;

/**
 * Configure {@link DirectRabbitListenerContainerFactoryConfigurer} with sensible
 * defaults.
 *
 * @author Gary Russell
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public final class DirectRabbitListenerContainerFactoryConfigurer
		extends AbstractRabbitListenerContainerFactoryConfigurer<DirectRabbitListenerContainerFactory> {

	/**
	 * Creates a new configurer that will use the given {@code rabbitProperties}.
	 * @param rabbitProperties properties to use
	 * @since 2.6.0
	 */
	public DirectRabbitListenerContainerFactoryConfigurer(RabbitProperties rabbitProperties) {
		super(rabbitProperties);
	}

	@Override
	public void configure(DirectRabbitListenerContainerFactory factory, ConnectionFactory connectionFactory) {
		PropertyMapper map = PropertyMapper.get();
		RabbitProperties.DirectContainer config = getRabbitProperties().getListener().getDirect();
		configure(factory, connectionFactory, config);
		map.from(config::getConsumersPerQueue).whenNonNull().to(factory::setConsumersPerQueue);
	}

}
