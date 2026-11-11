/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.amqp.rabbitmq.autoconfigure;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.ContainerCustomizer;
import org.springframework.amqp.rabbit.config.RabbitListenerConfigUtils;
import org.springframework.amqp.rabbitmq.client.AmqpConnectionFactory;
import org.springframework.amqp.rabbitmq.client.config.RabbitAmqpListenerContainerFactory;
import org.springframework.amqp.rabbitmq.client.listener.RabbitAmqpListenerContainer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;

/**
 * Configuration for Spring AMQP annotation-driven endpoints using RabbitMQ.
 *
 * @author Eddú Meléndez
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(EnableRabbit.class)
class AmqpRabbitAnnotationDrivenConfiguration {

	@Bean
	@ConditionalOnMissingBean
	RabbitAmqpListenerContainerFactoryConfigurer rabbitAmqpListenerContainerFactoryConfigurer(
			AmqpRabbitProperties properties, ObjectProvider<MessageConverter> messageConverter,
			ObjectProvider<TaskScheduler> taskSchedulers) {
		RabbitAmqpListenerContainerFactoryConfigurer configurer = new RabbitAmqpListenerContainerFactoryConfigurer(
				properties);
		messageConverter.ifAvailable(configurer::setMessageConverter);
		taskSchedulers.ifAvailable(configurer::setTaskScheduler);
		return configurer;
	}

	@Bean(name = "rabbitListenerContainerFactory")
	@ConditionalOnMissingBean(name = "rabbitListenerContainerFactory")
	RabbitAmqpListenerContainerFactory rabbitAmqpListenerContainerFactory(
			RabbitAmqpListenerContainerFactoryConfigurer configurer, AmqpConnectionFactory connectionFactory,
			ObjectProvider<ContainerCustomizer<RabbitAmqpListenerContainer>> amqpContainerCustomizer) {
		RabbitAmqpListenerContainerFactory factory = new RabbitAmqpListenerContainerFactory(connectionFactory);
		configurer.configure(factory);
		amqpContainerCustomizer.ifUnique(factory::setContainerCustomizer);
		return factory;
	}

	@Configuration(proxyBeanMethods = false)
	@EnableRabbit
	@ConditionalOnMissingBean(name = RabbitListenerConfigUtils.RABBIT_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME)
	static class EnableRabbitConfiguration {

	}

}
