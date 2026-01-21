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

import com.rabbitmq.stream.Environment;
import com.rabbitmq.stream.EnvironmentBuilder;

import org.springframework.amqp.rabbit.config.ContainerCustomizer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.amqp.autoconfigure.RabbitProperties.StreamContainer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.rabbit.stream.config.StreamRabbitListenerContainerFactory;
import org.springframework.rabbit.stream.listener.ConsumerCustomizer;
import org.springframework.rabbit.stream.listener.StreamListenerContainer;
import org.springframework.rabbit.stream.micrometer.RabbitStreamListenerObservationConvention;
import org.springframework.rabbit.stream.micrometer.RabbitStreamTemplateObservationConvention;
import org.springframework.rabbit.stream.producer.ProducerCustomizer;
import org.springframework.rabbit.stream.producer.RabbitStreamOperations;
import org.springframework.rabbit.stream.producer.RabbitStreamTemplate;
import org.springframework.rabbit.stream.support.converter.StreamMessageConverter;
import org.springframework.util.Assert;

/**
 * Configuration for Spring RabbitMQ Stream plugin support.
 *
 * @author Gary Russell
 * @author Eddú Meléndez
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(StreamRabbitListenerContainerFactory.class)
class RabbitStreamConfiguration {

	@Bean(name = "rabbitListenerContainerFactory")
	@ConditionalOnMissingBean(name = "rabbitListenerContainerFactory")
	@ConditionalOnProperty(name = "spring.rabbitmq.listener.type", havingValue = "stream")
	StreamRabbitListenerContainerFactory streamRabbitListenerContainerFactory(Environment rabbitStreamEnvironment,
			RabbitProperties properties, ObjectProvider<ConsumerCustomizer> consumerCustomizer,
			ObjectProvider<ContainerCustomizer<StreamListenerContainer>> containerCustomizer,
			ObjectProvider<RabbitStreamListenerObservationConvention> observationConvention) {
		StreamRabbitListenerContainerFactory factory = new StreamRabbitListenerContainerFactory(
				rabbitStreamEnvironment);
		StreamContainer stream = properties.getListener().getStream();
		factory.setObservationEnabled(stream.isObservationEnabled());
		factory.setNativeListener(stream.isNativeListener());
		observationConvention.ifUnique(factory::setStreamListenerObservationConvention);
		consumerCustomizer.ifUnique(factory::setConsumerCustomizer);
		containerCustomizer.ifUnique(factory::setContainerCustomizer);
		return factory;
	}

	@Bean(name = "rabbitStreamEnvironment")
	@ConditionalOnMissingBean(name = "rabbitStreamEnvironment")
	Environment rabbitStreamEnvironment(RabbitProperties properties, RabbitConnectionDetails connectionDetails,
			ObjectProvider<EnvironmentBuilderCustomizer> customizers) {
		EnvironmentBuilder builder = configure(Environment.builder(), properties, connectionDetails);
		customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
		return builder.build();
	}

	@Bean
	@ConditionalOnMissingBean
	RabbitStreamTemplateConfigurer rabbitStreamTemplateConfigurer(RabbitProperties properties,
			ObjectProvider<MessageConverter> messageConverter,
			ObjectProvider<StreamMessageConverter> streamMessageConverter,
			ObjectProvider<ProducerCustomizer> producerCustomizer,
			ObjectProvider<RabbitStreamTemplateObservationConvention> observationConvention) {
		RabbitStreamTemplateConfigurer configurer = new RabbitStreamTemplateConfigurer();
		configurer.setMessageConverter(messageConverter.getIfUnique());
		configurer.setStreamMessageConverter(streamMessageConverter.getIfUnique());
		configurer.setProducerCustomizer(producerCustomizer.getIfUnique());
		configurer.setObservationConvention(observationConvention.getIfUnique());
		return configurer;
	}

	@Bean
	@ConditionalOnMissingBean(RabbitStreamOperations.class)
	@ConditionalOnProperty(name = "spring.rabbitmq.stream.name")
	RabbitStreamTemplate rabbitStreamTemplate(Environment rabbitStreamEnvironment, RabbitProperties properties,
			RabbitStreamTemplateConfigurer configurer) {
		String name = properties.getStream().getName();
		Assert.state(name != null, "'name' must not be null");
		RabbitStreamTemplate template = new RabbitStreamTemplate(rabbitStreamEnvironment, name);
		configurer.configure(template);
		return template;
	}

	static EnvironmentBuilder configure(EnvironmentBuilder builder, RabbitProperties properties,
			RabbitConnectionDetails connectionDetails) {
		return configure(builder, properties.getStream(), connectionDetails);
	}

	private static EnvironmentBuilder configure(EnvironmentBuilder builder, RabbitProperties.Stream stream,
			RabbitConnectionDetails connectionDetails) {
		builder.lazyInitialization(true);
		PropertyMapper map = PropertyMapper.get();
		map.from(stream.getHost()).to(builder::host);
		map.from(stream.getPort()).to(builder::port);
		map.from(stream.getVirtualHost()).orFrom(connectionDetails::getVirtualHost).to(builder::virtualHost);
		map.from(stream.getUsername()).orFrom(connectionDetails::getUsername).to(builder::username);
		map.from(stream.getPassword()).orFrom(connectionDetails::getPassword).to(builder::password);
		return builder;
	}

}
