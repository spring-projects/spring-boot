/*
 * Copyright 2012-2024 the original author or authors.
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
import org.springframework.boot.autoconfigure.amqp.RabbitProperties.StreamContainer;
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
class RabbitStreamConfiguration {

	/**
     * Creates a RabbitListenerContainerFactory bean for stream type listeners.
     * This factory is conditionally created only if there is no existing bean with the same name.
     * It is also conditionally created based on the property "spring.rabbitmq.listener.type" having the value "stream".
     * 
     * @param rabbitStreamEnvironment The environment for RabbitMQ stream.
     * @param properties The RabbitMQ properties.
     * @param consumerCustomizer The customizer for the consumer.
     * @param containerCustomizer The customizer for the container.
     * @return The created StreamRabbitListenerContainerFactory bean.
     */
    @Bean(name = "rabbitListenerContainerFactory")
	@ConditionalOnMissingBean(name = "rabbitListenerContainerFactory")
	@ConditionalOnProperty(prefix = "spring.rabbitmq.listener", name = "type", havingValue = "stream")
	StreamRabbitListenerContainerFactory streamRabbitListenerContainerFactory(Environment rabbitStreamEnvironment,
			RabbitProperties properties, ObjectProvider<ConsumerCustomizer> consumerCustomizer,
			ObjectProvider<ContainerCustomizer<StreamListenerContainer>> containerCustomizer) {
		StreamRabbitListenerContainerFactory factory = new StreamRabbitListenerContainerFactory(
				rabbitStreamEnvironment);
		StreamContainer stream = properties.getListener().getStream();
		factory.setObservationEnabled(stream.isObservationEnabled());
		factory.setNativeListener(stream.isNativeListener());
		consumerCustomizer.ifUnique(factory::setConsumerCustomizer);
		containerCustomizer.ifUnique(factory::setContainerCustomizer);
		return factory;
	}

	/**
     * Creates and configures the RabbitStreamEnvironment bean.
     * 
     * @param properties the RabbitProperties object containing the RabbitMQ configuration properties
     * @param customizers the ObjectProvider of EnvironmentBuilderCustomizer objects for customizing the environment builder
     * @return the RabbitStreamEnvironment bean
     */
    @Bean(name = "rabbitStreamEnvironment")
	@ConditionalOnMissingBean(name = "rabbitStreamEnvironment")
	Environment rabbitStreamEnvironment(RabbitProperties properties,
			ObjectProvider<EnvironmentBuilderCustomizer> customizers) {
		EnvironmentBuilder builder = configure(Environment.builder(), properties);
		customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
		return builder.build();
	}

	/**
     * Creates a RabbitStreamTemplateConfigurer bean if there is no existing bean of the same type.
     * This configurer is responsible for configuring the RabbitStreamTemplate with the necessary properties.
     * 
     * @param properties The RabbitProperties object containing the RabbitMQ configuration properties.
     * @param messageConverter The MessageConverter object used for converting messages.
     * @param streamMessageConverter The StreamMessageConverter object used for converting stream messages.
     * @param producerCustomizer The ProducerCustomizer object used for customizing the producer.
     * @return The RabbitStreamTemplateConfigurer bean.
     */
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

	/**
     * Creates a RabbitStreamTemplate bean if there is no existing bean of type RabbitStreamOperations.
     * The creation of the bean is conditional on the presence of the property "spring.rabbitmq.stream.name".
     * 
     * @param rabbitStreamEnvironment the environment for RabbitMQ stream
     * @param properties the RabbitMQ properties
     * @param configurer the RabbitStreamTemplateConfigurer
     * @return the RabbitStreamTemplate bean
     */
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

	/**
     * Configures the RabbitMQ environment builder with the provided RabbitProperties.
     * 
     * @param builder the RabbitMQ environment builder to configure
     * @param properties the RabbitProperties containing the configuration values
     * @return the configured RabbitMQ environment builder
     */
    static EnvironmentBuilder configure(EnvironmentBuilder builder, RabbitProperties properties) {
		builder.lazyInitialization(true);
		RabbitProperties.Stream stream = properties.getStream();
		PropertyMapper map = PropertyMapper.get();
		map.from(stream.getHost()).to(builder::host);
		map.from(stream.getPort()).to(builder::port);
		map.from(stream.getVirtualHost())
			.as(withFallback(properties::getVirtualHost))
			.whenNonNull()
			.to(builder::virtualHost);
		map.from(stream.getUsername()).as(withFallback(properties::getUsername)).whenNonNull().to(builder::username);
		map.from(stream.getPassword()).as(withFallback(properties::getPassword)).whenNonNull().to(builder::password);
		return builder;
	}

	/**
     * Returns a Function that takes a String value and returns it if it is not null,
     * otherwise returns the value obtained from the provided fallback Supplier.
     *
     * @param fallback the Supplier used to obtain the fallback value if the input value is null
     * @return a Function that performs the fallback logic
     */
    private static Function<String, String> withFallback(Supplier<String> fallback) {
		return (value) -> (value != null) ? value : fallback.get();
	}

}
