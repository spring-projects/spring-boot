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

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.DirectRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.RabbitListenerConfigUtils;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Spring AMQP annotation driven endpoints.
 *
 * @author Stephane Nicoll
 * @author Josh Thornhill
 */
@Configuration
@ConditionalOnClass(EnableRabbit.class)
class RabbitAnnotationDrivenConfiguration {

	private final ObjectProvider<MessageConverter> messageConverter;

	private final ObjectProvider<MessageRecoverer> messageRecoverer;

	private final RabbitProperties properties;

	RabbitAnnotationDrivenConfiguration(ObjectProvider<MessageConverter> messageConverter,
			ObjectProvider<MessageRecoverer> messageRecoverer, RabbitProperties properties) {
		this.messageConverter = messageConverter;
		this.messageRecoverer = messageRecoverer;
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	public SimpleRabbitListenerContainerFactoryConfigurer simpleRabbitListenerContainerFactoryConfigurer() {
		SimpleRabbitListenerContainerFactoryConfigurer configurer = new SimpleRabbitListenerContainerFactoryConfigurer();
		configurer.setMessageConverter(this.messageConverter.getIfUnique());
		configurer.setMessageRecoverer(this.messageRecoverer.getIfUnique());
		configurer.setRabbitProperties(this.properties);
		return configurer;
	}

	@Bean(name = "rabbitListenerContainerFactory")
	@ConditionalOnMissingBean(name = "rabbitListenerContainerFactory")
	@ConditionalOnProperty(prefix = "spring.rabbitmq.listener", name = "type", havingValue = "simple",
			matchIfMissing = true)
	public SimpleRabbitListenerContainerFactory simpleRabbitListenerContainerFactory(
			SimpleRabbitListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory) {
		SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
		configurer.configure(factory, connectionFactory);
		return factory;
	}

	@Bean
	@ConditionalOnMissingBean
	public DirectRabbitListenerContainerFactoryConfigurer directRabbitListenerContainerFactoryConfigurer() {
		DirectRabbitListenerContainerFactoryConfigurer configurer = new DirectRabbitListenerContainerFactoryConfigurer();
		configurer.setMessageConverter(this.messageConverter.getIfUnique());
		configurer.setMessageRecoverer(this.messageRecoverer.getIfUnique());
		configurer.setRabbitProperties(this.properties);
		return configurer;
	}

	@Bean(name = "rabbitListenerContainerFactory")
	@ConditionalOnMissingBean(name = "rabbitListenerContainerFactory")
	@ConditionalOnProperty(prefix = "spring.rabbitmq.listener", name = "type", havingValue = "direct")
	public DirectRabbitListenerContainerFactory directRabbitListenerContainerFactory(
			DirectRabbitListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory) {
		DirectRabbitListenerContainerFactory factory = new DirectRabbitListenerContainerFactory();
		configurer.configure(factory, connectionFactory);
		return factory;
	}

	@Configuration
	@EnableRabbit
	@ConditionalOnMissingBean(name = RabbitListenerConfigUtils.RABBIT_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME)
	protected static class EnableRabbitConfiguration {

	}

}
