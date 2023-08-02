/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.kafka;

import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.MessageListenerContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Tests for {@link ConcurrentKafkaListenerContainerFactoryConfigurer}.
 *
 * @author Moritz Halbritter
 */
class ConcurrentKafkaListenerContainerFactoryConfigurerTests {

	private ConcurrentKafkaListenerContainerFactoryConfigurer configurer;

	private ConcurrentKafkaListenerContainerFactory<Object, Object> factory;

	private ConsumerFactory<Object, Object> consumerFactory;

	private KafkaProperties properties;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		this.configurer = new ConcurrentKafkaListenerContainerFactoryConfigurer();
		this.properties = new KafkaProperties();
		this.configurer.setKafkaProperties(this.properties);
		this.factory = spy(new ConcurrentKafkaListenerContainerFactory<>());
		this.consumerFactory = mock(ConsumerFactory.class);

	}

	@Test
	void shouldApplyThreadNameSupplier() {
		Function<MessageListenerContainer, String> function = (container) -> "thread-1";
		this.configurer.setThreadNameSupplier(function);
		this.configurer.configure(this.factory, this.consumerFactory);
		then(this.factory).should().setThreadNameSupplier(function);
	}

	@Test
	void shouldApplyChangeConsumerThreadName() {
		this.properties.getListener().setChangeConsumerThreadName(true);
		this.configurer.configure(this.factory, this.consumerFactory);
		then(this.factory).should().setChangeConsumerThreadName(true);
	}

	@Test
	void shouldApplyListenerTaskExecutor() {
		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
		this.configurer.setListenerTaskExecutor(executor);
		this.configurer.configure(this.factory, this.consumerFactory);
		assertThat(this.factory.getContainerProperties().getListenerTaskExecutor()).isEqualTo(executor);
	}

}
