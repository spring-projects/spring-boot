/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.amqp.autoconfigure;

import java.util.concurrent.Executor;

import org.jspecify.annotations.Nullable;

import org.springframework.amqp.client.config.AmqpMessageListenerContainerFactory;
import org.springframework.amqp.client.config.MethodAmqpMessageListenerContainerFactory;
import org.springframework.amqp.client.listener.AmqpListenerErrorHandler;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.amqp.autoconfigure.AmqpProperties.Listener;
import org.springframework.boot.context.properties.PropertyMapper;

/**
 * Configure {@link AmqpMessageListenerContainerFactory} with sensible defaults tuned
 * using configuration properties. Handle additional settings exposed by
 * {@link MethodAmqpMessageListenerContainerFactory} as well.
 * <p>
 * Can be injected into application code and used to define a custom
 * {@code AmqpMessageListenerContainerFactory} whose configuration is based upon that
 * produced by auto-configuration.
 *
 * @author Stephane Nicoll
 * @since 4.2.0
 */
public class AmqpMessageListenerContainerFactoryConfigurer {

	private final AmqpProperties amqpProperties;

	private @Nullable Executor taskExecutor;

	private @Nullable MessageConverter messageConverter;

	private @Nullable AmqpListenerErrorHandler listenerErrorHandler;

	public AmqpMessageListenerContainerFactoryConfigurer(AmqpProperties amqpProperties) {
		this.amqpProperties = amqpProperties;
	}

	protected void setTaskExecutor(@Nullable Executor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	protected void setMessageConverter(@Nullable MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	protected void setListenerErrorHandler(@Nullable AmqpListenerErrorHandler listenerErrorHandler) {
		this.listenerErrorHandler = listenerErrorHandler;
	}

	public void configure(AmqpMessageListenerContainerFactory factory) {
		PropertyMapper map = PropertyMapper.get();
		Listener listener = this.amqpProperties.getListener();

		map.from(listener::isAutoStartup).to(factory::setAutoStartup);
		map.from(listener::getConcurrency).to(factory::setConcurrency);
		map.from(listener::getInitialCredits).to(factory::setInitialCredits);
		map.from(listener::getReceiveTimeout).to(factory::setReceiveTimeout);
		map.from(listener::getGracefulShutdownPeriod).to(factory::setGracefulShutdownPeriod);
		map.from(this.taskExecutor).to(factory::setTaskExecutor);

		if (factory instanceof MethodAmqpMessageListenerContainerFactory methodFactory) {
			map.from(listener::isDefaultRequeueRejected).to(methodFactory::setDefaultRequeueRejected);
			map.from(this.messageConverter).to(methodFactory::setMessageConverter);
			map.from(this.listenerErrorHandler).to(methodFactory::setListenerErrorHandler);
		}
	}

}
