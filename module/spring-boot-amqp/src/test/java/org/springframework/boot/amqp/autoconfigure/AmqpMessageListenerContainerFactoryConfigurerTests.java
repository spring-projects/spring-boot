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

import java.time.Duration;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.Test;

import org.springframework.amqp.client.config.AmqpMessageListenerContainerFactory;
import org.springframework.amqp.client.config.MethodAmqpMessageListenerContainerFactory;
import org.springframework.amqp.client.listener.AmqpListenerErrorHandler;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.amqp.autoconfigure.AmqpProperties.Listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * Tests for {@link AmqpMessageListenerContainerFactoryConfigurer}.
 *
 * @author Stephane Nicoll
 */
class AmqpMessageListenerContainerFactoryConfigurerTests {

	@Test
	void configureAppliesListenerPropertiesToAmqpMessageListenerContainerFactory() {
		AmqpProperties properties = new AmqpProperties();
		Listener listener = properties.getListener();
		listener.setAutoStartup(false);
		listener.setConcurrency(3);
		listener.setInitialCredits(50);
		listener.setReceiveTimeout(Duration.ofMillis(500));
		listener.setGracefulShutdownPeriod(Duration.ofSeconds(60));
		Executor taskExecutor = mock(Executor.class);
		AmqpMessageListenerContainerFactoryConfigurer configurer = new AmqpMessageListenerContainerFactoryConfigurer(
				properties);
		configurer.setTaskExecutor(taskExecutor);

		AmqpMessageListenerContainerFactory factory = mock(AmqpMessageListenerContainerFactory.class);
		configurer.configure(factory);
		then(factory).should().setAutoStartup(false);
		then(factory).should().setConcurrency(3);
		then(factory).should().setInitialCredits(50);
		then(factory).should().setReceiveTimeout(Duration.ofMillis(500));
		then(factory).should().setGracefulShutdownPeriod(Duration.ofSeconds(60));
		then(factory).should().setTaskExecutor(taskExecutor);
	}

	@Test
	void configureAppliesMethodAmqpMessageListenerContainerFactoryProperties() {
		AmqpProperties properties = new AmqpProperties();
		properties.getListener().setDefaultRequeueRejected(false);

		AmqpMessageListenerContainerFactoryConfigurer configurer = new AmqpMessageListenerContainerFactoryConfigurer(
				properties);
		MessageConverter messageConverter = mock(MessageConverter.class);
		AmqpListenerErrorHandler errorHandler = mock(AmqpListenerErrorHandler.class);
		configurer.setMessageConverter(messageConverter);
		configurer.setListenerErrorHandler(errorHandler);

		MethodAmqpMessageListenerContainerFactory factory = mock(MethodAmqpMessageListenerContainerFactory.class);
		configurer.configure(factory);

		then(factory).should().setDefaultRequeueRejected(false);
		then(factory).should().setMessageConverter(messageConverter);
		then(factory).should().setListenerErrorHandler(errorHandler);
	}

	@Test
	void configureSkipsOptionalDependenciesWhenNotSet() {
		AmqpProperties properties = new AmqpProperties();
		AmqpMessageListenerContainerFactoryConfigurer configurer = new AmqpMessageListenerContainerFactoryConfigurer(
				properties);

		MethodAmqpMessageListenerContainerFactory factory = mock(MethodAmqpMessageListenerContainerFactory.class);
		configurer.configure(factory);

		then(factory).should(never()).setTaskExecutor(any());
		then(factory).should(never()).setMessageConverter(any());
		then(factory).should(never()).setListenerErrorHandler(any());
		then(factory).should().setDefaultRequeueRejected(properties.getListener().isDefaultRequeueRejected());
	}

}
