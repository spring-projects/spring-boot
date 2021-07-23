/*
 * Copyright 2021-2021 the original author or authors.
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

import com.rabbitmq.stream.Environment;
import com.rabbitmq.stream.EnvironmentBuilder;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.ContainerCustomizer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.rabbit.stream.config.StreamRabbitListenerContainerFactory;
import org.springframework.rabbit.stream.listener.ConsumerCustomizer;
import org.springframework.rabbit.stream.listener.StreamListenerContainer;

/**
 * Configuration for Spring RabbitMQ Stream Plugin support.
 *
 * @author Gary Russell
 * @since 2.6
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(EnableRabbit.class)
@ConditionalOnProperty(prefix = "spring.rabbitmq.listener", name = "type", havingValue = "stream")
public class RabbitStreamConfiguration {

	@Bean(name = "rabbitListenerContainerFactory")
	@ConditionalOnMissingBean(name = "rabbitListenerContainerFactory")
	StreamRabbitListenerContainerFactory streamRabbitListenerContainerFactory(Environment rabbitStreamEnvironment,
			RabbitProperties properties, ObjectProvider<ConsumerCustomizer> consumerCustomizers,
			ObjectProvider<ContainerCustomizer<StreamListenerContainer>> containerCustomizers) {

		StreamRabbitListenerContainerFactory factory = new StreamRabbitListenerContainerFactory(
				rabbitStreamEnvironment);
		factory.setNativeListener(properties.getListener().getStream().isNativeListener());
		if (consumerCustomizers.getIfUnique() != null) {
			factory.setConsumerCustomizer(consumerCustomizers.getIfUnique());
		}
		if (containerCustomizers.getIfUnique() != null) {
			factory.setContainerCustomizer(containerCustomizers.getIfUnique());
		}
		return factory;
	}

	@Bean(name = "rabbitStreamEnvironment")
	@ConditionalOnMissingBean(name = "rabbitStreamEnvironment")
	Environment rabbitStreamEnvironment(RabbitProperties properties) {
		RabbitProperties.Stream stream = properties.getStream();
		String username = stream.getUserName();
		if (username == null) {
			username = properties.getUsername();
		}
		String password = stream.getPassword();
		if (password == null) {
			password = properties.getPassword();
		}
		EnvironmentBuilder builder = Environment.builder().lazyInitialization(true).host(stream.getHost())
				.port(stream.getPort());
		if (username != null) {
			builder.username(username);
		}
		if (password != null) {
			builder.password(password);
		}
		return builder.build();
	}

}
