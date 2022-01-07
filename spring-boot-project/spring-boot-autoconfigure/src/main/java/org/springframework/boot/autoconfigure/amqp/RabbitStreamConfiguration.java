/*
 * Copyright 2012-2022 the original author or authors.
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

import java.util.function.Function;
import java.util.function.Supplier;

import com.rabbitmq.stream.Environment;
import com.rabbitmq.stream.EnvironmentBuilder;

import org.springframework.amqp.rabbit.config.ContainerCustomizer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.rabbit.stream.config.StreamRabbitListenerContainerFactory;
import org.springframework.rabbit.stream.listener.ConsumerCustomizer;
import org.springframework.rabbit.stream.listener.StreamListenerContainer;
import org.springframework.rabbit.stream.producer.ProducerCustomizer;
import org.springframework.rabbit.stream.producer.RabbitStreamOperations;
import org.springframework.rabbit.stream.producer.RabbitStreamTemplate;
import org.springframework.rabbit.stream.support.converter.StreamMessageConverter;

/**
 * Configuration for Spring RabbitMQ Stream plugin support.
 *
 * @author Gary Russell
 * @author Eddú Meléndez
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(StreamRabbitListenerContainerFactory.class)
@ConditionalOnProperty(prefix = "spring.rabbitmq.listener", name = "type", havingValue = "stream")
class RabbitStreamConfiguration {

	@Bean(name = "rabbitListenerContainerFactory")
	@ConditionalOnMissingBean(name = "rabbitListenerContainerFactory")
	StreamRabbitListenerContainerFactory streamRabbitListenerContainerFactory(Environment rabbitStreamEnvironment,
			RabbitProperties properties, ObjectProvider<ConsumerCustomizer> consumerCustomizer,
			ObjectProvider<ContainerCustomizer<StreamListenerContainer>> containerCustomizer) {
		StreamRabbitListenerContainerFactory factory = new StreamRabbitListenerContainerFactory(
				rabbitStreamEnvironment);
		factory.setNativeListener(properties.getListener().getStream().isNativeListener());
		consumerCustomizer.ifUnique(factory::setConsumerCustomizer);
		containerCustomizer.ifUnique(factory::setContainerCustomizer);
		return factory;
	}

	@Bean(name = "rabbitStreamEnvironment")
	@ConditionalOnMissingBean(name = "rabbitStreamEnvironment")
	Environment rabbitStreamEnvironment(RabbitProperties properties) {
		return configure(Environment.builder(), properties).build();
	}

	@Bean
	@ConditionalOnMissingBean
	RabbitStreamTemplateConfigurer rabbitStreamTemplateConfigurer(RabbitProperties properties,
			ObjectProvider<MessageConverter> messageConverter,
			ObjectProvider<StreamMessageConverter> streamMessageConverter,
			ObjectProvider<ProducerCustomizer> producerCustomizer) {
		RabbitStreamTemplateConfigurer configurer = new RabbitStreamTemplateConfigurer();
		configurer.setMessageConverter(messageConverter.getIfUnique());
		configurer.setStreamMessageConverter(streamMessageConverter.getIfUnique());
		configurer.setProducerCustomizer(producerCustomizer.getIfUnique());
		return configurer;
	}

	@Bean
	@ConditionalOnMissingBean(RabbitStreamOperations.class)
	@ConditionalOnProperty(prefix = "spring.rabbitmq.stream", name = "name")
	RabbitStreamTemplate rabbitStreamTemplate(Environment rabbitStreamEnvironment, RabbitProperties properties,
			RabbitStreamTemplateConfigurer configurer) {
		RabbitStreamTemplate template = new RabbitStreamTemplate(rabbitStreamEnvironment,
				properties.getStream().getName());
		configurer.configure(template);
		return template;
	}

	static EnvironmentBuilder configure(EnvironmentBuilder builder, RabbitProperties properties) {
		builder.lazyInitialization(true);
		RabbitProperties.Stream stream = properties.getStream();
		PropertyMapper mapper = PropertyMapper.get();
		mapper.from(stream.getHost()).to(builder::host);
		mapper.from(stream.getPort()).to(builder::port);
		mapper.from(stream.getUsername()).as(withFallback(properties::getUsername)).whenNonNull().to(builder::username);
		mapper.from(stream.getPassword()).as(withFallback(properties::getPassword)).whenNonNull().to(builder::password);
		return builder;
	}

	private static Function<String, String> withFallback(Supplier<String> fallback) {
		return (value) -> (value != null) ? value : fallback.get();
	}

}
