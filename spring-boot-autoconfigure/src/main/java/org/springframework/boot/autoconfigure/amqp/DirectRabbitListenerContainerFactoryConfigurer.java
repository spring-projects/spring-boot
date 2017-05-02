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

import org.springframework.amqp.rabbit.config.DirectRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;

/**
 * Configure {@link RabbitListenerContainerFactory} with sensible defaults.
 *
 * @author Gary Russell
 * @since 2.0
 */
public final class DirectRabbitListenerContainerFactoryConfigurer
		extends AbstractRabbitListenerContainerFactoryConfigurer<DirectRabbitListenerContainerFactory> {

	@Override
	protected void configure(DirectRabbitListenerContainerFactory factory, RabbitProperties rabbitProperties) {
		RabbitProperties.Listener listenerConfig = rabbitProperties.getListener();
		if (listenerConfig.getConsumersPerQueue() != null) {
			factory.setConsumersPerQueue(listenerConfig.getConsumersPerQueue());
		}
	}

}
