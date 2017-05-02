/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.amqp;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;

/**
 * Configure {@link RabbitListenerContainerFactory} with sensible defaults.
 *
 * @author Stephane Nicoll
 * @author Gary Russell
 * @since 1.3.3
 */
public final class SimpleRabbitListenerContainerFactoryConfigurer
		extends AbstractRabbitListenerContainerFactoryConfigurer<SimpleRabbitListenerContainerFactory> {

	@Override
	protected void configure(SimpleRabbitListenerContainerFactory factory, RabbitProperties rabbitProperties) {
		RabbitProperties.Listener listenerConfig = rabbitProperties.getListener();
		if (listenerConfig.getConcurrency() != null) {
			factory.setConcurrentConsumers(listenerConfig.getConcurrency());
		}
		if (listenerConfig.getMaxConcurrency() != null) {
			factory.setMaxConcurrentConsumers(listenerConfig.getMaxConcurrency());
		}
		if (listenerConfig.getTransactionSize() != null) {
			factory.setTxSize(listenerConfig.getTransactionSize());
		}
	}

}
