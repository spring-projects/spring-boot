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

import org.springframework.amqp.client.AmqpConnectionFactory;
import org.springframework.amqp.client.annotation.AmqpListener;
import org.springframework.amqp.client.config.AmqpDefaultConfiguration;
import org.springframework.amqp.client.config.EnableAmqp;
import org.springframework.amqp.client.config.MethodAmqpMessageListenerContainerFactory;
import org.springframework.amqp.client.listener.AmqpListenerErrorHandler;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnThreading;
import org.springframework.boot.thread.Threading;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.VirtualThreadTaskExecutor;

/**
 * Configuration for Spring AMQP annotation driven endpoints using {@link AmqpListener}.
 *
 * @author Stephane Nicoll
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(EnableAmqp.class)
class AmqpAnnotationDrivenConfiguration {

	private static final String AMQP_LISTENER_CONTAINER_FACTORY_CONFIGURER_BEAN_NAME = "amqpMessageListenerContainerFactoryConfigurer";

	private final AmqpProperties properties;

	private final ObjectProvider<MessageConverter> messageConverter;

	private final ObjectProvider<AmqpListenerErrorHandler> listenerErrorHandler;

	AmqpAnnotationDrivenConfiguration(AmqpProperties properties, ObjectProvider<MessageConverter> messageConverter,
			ObjectProvider<AmqpListenerErrorHandler> listenerErrorHandler) {
		this.properties = properties;
		this.messageConverter = messageConverter;
		this.listenerErrorHandler = listenerErrorHandler;
	}

	@Bean(name = AMQP_LISTENER_CONTAINER_FACTORY_CONFIGURER_BEAN_NAME)
	@ConditionalOnMissingBean
	@ConditionalOnThreading(Threading.PLATFORM)
	AmqpMessageListenerContainerFactoryConfigurer amqpMessageListenerContainerFactoryConfigurer() {
		return messageListenerConfigurer();
	}

	@Bean(name = AMQP_LISTENER_CONTAINER_FACTORY_CONFIGURER_BEAN_NAME)
	@ConditionalOnMissingBean
	@ConditionalOnThreading(Threading.VIRTUAL)
	AmqpMessageListenerContainerFactoryConfigurer amqpMessageListenerContainerFactoryConfigurerVirtualThreads() {
		AmqpMessageListenerContainerFactoryConfigurer configurer = messageListenerConfigurer();
		configurer.setTaskExecutor(new VirtualThreadTaskExecutor("amqp-simple-"));
		return configurer;
	}

	@Bean(name = AmqpDefaultConfiguration.DEFAULT_AMQP_LISTENER_CONTAINER_FACTORY_BEAN_NAME)
	@ConditionalOnMissingBean(name = AmqpDefaultConfiguration.DEFAULT_AMQP_LISTENER_CONTAINER_FACTORY_BEAN_NAME)
	MethodAmqpMessageListenerContainerFactory amqpMessageListenerContainerFactory(
			AmqpMessageListenerContainerFactoryConfigurer configurer, AmqpConnectionFactory connectionFactory) {
		MethodAmqpMessageListenerContainerFactory factory = new MethodAmqpMessageListenerContainerFactory(
				connectionFactory);
		configurer.configure(factory);
		return factory;
	}

	private AmqpMessageListenerContainerFactoryConfigurer messageListenerConfigurer() {
		AmqpMessageListenerContainerFactoryConfigurer configurer = new AmqpMessageListenerContainerFactoryConfigurer(
				this.properties);
		configurer.setMessageConverter(this.messageConverter.getIfUnique());
		configurer.setListenerErrorHandler(this.listenerErrorHandler.getIfUnique());
		return configurer;
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAmqp
	@ConditionalOnMissingBean(name = AmqpDefaultConfiguration.AMQP_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME)
	static class EnableAmqpConfiguration {

	}

}
