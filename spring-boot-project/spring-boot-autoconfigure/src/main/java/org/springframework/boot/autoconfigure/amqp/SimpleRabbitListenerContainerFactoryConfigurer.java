/*
 * Copyright 2012-2019 the original author or authors.
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

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.context.properties.PropertyMapper;

/**
 * Configure {@link SimpleRabbitListenerContainerFactoryConfigurer} with sensible
 * defaults.
 *
 * @author Stephane Nicoll
 * @author Gary Russell
 * @since 1.3.3
 */
public final class SimpleRabbitListenerContainerFactoryConfigurer
		extends AbstractRabbitListenerContainerFactoryConfigurer<SimpleRabbitListenerContainerFactory> {

	@Override
	public void configure(SimpleRabbitListenerContainerFactory factory, ConnectionFactory connectionFactory) {
		PropertyMapper map = PropertyMapper.get();
		RabbitProperties.SimpleContainer config = getRabbitProperties().getListener().getSimple();
		configure(factory, connectionFactory, config);
		map.from(config::getConcurrency).whenNonNull().to(factory::setConcurrentConsumers);
		map.from(config::getMaxConcurrency).whenNonNull().to(factory::setMaxConcurrentConsumers);
		map.from(config::getBatchSize).whenNonNull().to(factory::setBatchSize);
	}

}
