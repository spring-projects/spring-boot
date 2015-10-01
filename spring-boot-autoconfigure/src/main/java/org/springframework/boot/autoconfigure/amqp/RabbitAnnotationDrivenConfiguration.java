/*
 * Copyright 2012-2015 the original author or authors.
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

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.RabbitListenerConfigUtils;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties.Listener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Spring AMQP annotation driven endpoints.
 *
 * @author Stephane Nicoll
 * @author Josh Thornhill
 * @since 1.2.0
 */
@Configuration
@ConditionalOnClass(EnableRabbit.class)
class RabbitAnnotationDrivenConfiguration {

	@Bean
	@ConditionalOnMissingBean(name = "rabbitListenerContainerFactory")
	public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
			ConnectionFactory connectionFactory, RabbitProperties config) {
		SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
		factory.setConnectionFactory(connectionFactory);
		Listener listenerConfig = config.getListener();
		factory.setAutoStartup(listenerConfig.isAutoStartup());
		if (listenerConfig.getAcknowledgeMode() != null) {
			factory.setAcknowledgeMode(listenerConfig.getAcknowledgeMode());
		}
		if (listenerConfig.getConcurrency() != null) {
			factory.setConcurrentConsumers(listenerConfig.getConcurrency());
		}
		if (listenerConfig.getMaxConcurrency() != null) {
			factory.setMaxConcurrentConsumers(listenerConfig.getMaxConcurrency());
		}
		if (listenerConfig.getPrefetch() != null) {
			factory.setPrefetchCount(listenerConfig.getPrefetch());
		}
		if (listenerConfig.getTransactionSize() != null) {
			factory.setTxSize(listenerConfig.getTransactionSize());
		}
		return factory;
	}

	@EnableRabbit
	@ConditionalOnMissingBean(name = RabbitListenerConfigUtils.RABBIT_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME)
	protected static class EnableRabbitConfiguration {

	}

}
