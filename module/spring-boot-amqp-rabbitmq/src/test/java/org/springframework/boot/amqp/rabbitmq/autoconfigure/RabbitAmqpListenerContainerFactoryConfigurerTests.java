/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.amqp.rabbitmq.autoconfigure;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.springframework.amqp.rabbitmq.client.config.RabbitAmqpListenerContainerFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.amqp.rabbitmq.autoconfigure.AmqpRabbitProperties.Listener.Batch;
import org.springframework.scheduling.TaskScheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * Tests for {@link RabbitAmqpListenerContainerFactoryConfigurer}.
 *
 * @author Stephane Nicoll
 */
class RabbitAmqpListenerContainerFactoryConfigurerTests {

	@Test
	void configureAppliesDefaultListenerProperties() {
		AmqpRabbitProperties properties = new AmqpRabbitProperties();
		RabbitAmqpListenerContainerFactoryConfigurer configurer = new RabbitAmqpListenerContainerFactoryConfigurer(
				properties);
		RabbitAmqpListenerContainerFactory factory = mock(RabbitAmqpListenerContainerFactory.class);
		configurer.configure(factory);
		then(factory).should().setObservationEnabled(false);
		then(factory).should().setBatchReceiveTimeout(Duration.ofSeconds(30).toMillis());
		then(factory).should(never()).setBatchSize(any());
		then(factory).should(never()).setMessageConverter(any());
		then(factory).should(never()).setTaskScheduler(any());
	}

	@Test
	void configureAppliesCustomListenerProperties() {
		AmqpRabbitProperties properties = new AmqpRabbitProperties();
		properties.getListener().setObservationEnabled(true);
		Batch batch = properties.getListener().getBatch();
		batch.setSize(10);
		batch.setReceiveTimeout(Duration.ofSeconds(5));
		RabbitAmqpListenerContainerFactoryConfigurer configurer = new RabbitAmqpListenerContainerFactoryConfigurer(
				properties);
		RabbitAmqpListenerContainerFactory factory = mock(RabbitAmqpListenerContainerFactory.class);
		configurer.configure(factory);
		then(factory).should().setObservationEnabled(true);
		then(factory).should().setBatchSize(10);
		then(factory).should().setBatchReceiveTimeout(Duration.ofSeconds(5).toMillis());
	}

	@Test
	void configureAppliesMessageConverter() {
		AmqpRabbitProperties properties = new AmqpRabbitProperties();
		RabbitAmqpListenerContainerFactoryConfigurer configurer = new RabbitAmqpListenerContainerFactoryConfigurer(
				properties);
		MessageConverter messageConverter = mock(MessageConverter.class);
		configurer.setMessageConverter(messageConverter);
		RabbitAmqpListenerContainerFactory factory = mock(RabbitAmqpListenerContainerFactory.class);
		configurer.configure(factory);
		then(factory).should().setMessageConverter(messageConverter);
	}

	@Test
	void configureAppliesTaskScheduler() {
		AmqpRabbitProperties properties = new AmqpRabbitProperties();
		RabbitAmqpListenerContainerFactoryConfigurer configurer = new RabbitAmqpListenerContainerFactoryConfigurer(
				properties);
		TaskScheduler taskScheduler = mock(TaskScheduler.class);
		configurer.setTaskScheduler(taskScheduler);
		RabbitAmqpListenerContainerFactory factory = mock(RabbitAmqpListenerContainerFactory.class);
		configurer.configure(factory);
		then(factory).should().setTaskScheduler(taskScheduler);
	}

	@Test
	void configureSkipsOptionalDependenciesWhenNotSet() {
		AmqpRabbitProperties properties = new AmqpRabbitProperties();
		RabbitAmqpListenerContainerFactoryConfigurer configurer = new RabbitAmqpListenerContainerFactoryConfigurer(
				properties);
		RabbitAmqpListenerContainerFactory factory = mock(RabbitAmqpListenerContainerFactory.class);
		configurer.configure(factory);
		then(factory).should(never()).setMessageConverter(any());
		then(factory).should(never()).setTaskScheduler(any());
		then(factory).should(never()).setBatchSize(any());
	}

}
