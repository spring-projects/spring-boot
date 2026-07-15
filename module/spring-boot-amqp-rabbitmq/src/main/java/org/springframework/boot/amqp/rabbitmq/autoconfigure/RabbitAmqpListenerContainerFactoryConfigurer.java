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

package org.springframework.boot.amqp.rabbitmq.autoconfigure;

import java.time.Duration;

import org.jspecify.annotations.Nullable;

import org.springframework.amqp.rabbitmq.client.config.RabbitAmqpListenerContainerFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.amqp.rabbitmq.autoconfigure.AmqpRabbitProperties.Listener;
import org.springframework.boot.amqp.rabbitmq.autoconfigure.AmqpRabbitProperties.Listener.Batch;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;

/**
 * Configure {@link RabbitAmqpListenerContainerFactory} with sensible defaults tuned using
 * configuration properties.
 * <p>
 * Can be injected into application code and used to define a custom
 * {@code RabbitAmqpListenerContainerFactory} whose configuration is based upon that
 * produced by auto-configuration.
 *
 * @author Stephane Nicoll
 * @since 4.2.0
 */
public final class RabbitAmqpListenerContainerFactoryConfigurer {

	private final AmqpRabbitProperties properties;

	private @Nullable MessageConverter messageConverter;

	private @Nullable TaskScheduler taskScheduler;

	/**
	 * Creates a new configurer that will use the given {@code properties}.
	 * @param properties the properties to use
	 */
	public RabbitAmqpListenerContainerFactoryConfigurer(AmqpRabbitProperties properties) {
		this.properties = properties;
	}

	/**
	 * Set the {@link MessageConverter} to use or {@code null} if the out-of-the-box
	 * converter should be used.
	 * @param messageConverter the {@link MessageConverter}
	 */
	protected void setMessageConverter(@Nullable MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	/**
	 * Set the {@link TaskScheduler} to use or {@code null} if the default should be used.
	 * @param taskScheduler the {@link TaskScheduler}
	 */
	protected void setTaskScheduler(@Nullable TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
	}

	/**
	 * Configure the specified {@link RabbitAmqpListenerContainerFactory}. The factory can
	 * be further tuned and default settings can be overridden.
	 * @param factory the {@link RabbitAmqpListenerContainerFactory} instance to configure
	 */
	public void configure(RabbitAmqpListenerContainerFactory factory) {
		Assert.notNull(factory, "'factory' must not be null");
		PropertyMapper map = PropertyMapper.get();
		map.from(this.messageConverter).to(factory::setMessageConverter);
		map.from(this.taskScheduler).to(factory::setTaskScheduler);
		Listener listenerProperties = this.properties.getListener();
		map.from(listenerProperties.isObservationEnabled()).to(factory::setObservationEnabled);
		Batch batchProperties = listenerProperties.getBatch();
		map.from(batchProperties.getSize()).to(factory::setBatchSize);
		map.from(batchProperties.getReceiveTimeout()).as(Duration::toMillis).to(factory::setBatchReceiveTimeout);
	}

}
