/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.amqp;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.RabbitListenerConfigUtils;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link RabbitAutoConfiguration}.
 *
 * @author Greg Turnquist
 */
public class RabbitAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testDefaultRabbitConfiguration() {
		load(TestConfiguration.class);
		RabbitTemplate rabbitTemplate = this.context.getBean(RabbitTemplate.class);
		RabbitMessagingTemplate messagingTemplate = this.context
				.getBean(RabbitMessagingTemplate.class);
		CachingConnectionFactory connectionFactory = this.context
				.getBean(CachingConnectionFactory.class);
		RabbitAdmin amqpAdmin = this.context.getBean(RabbitAdmin.class);
		assertEquals(connectionFactory, rabbitTemplate.getConnectionFactory());
		assertEquals(rabbitTemplate, messagingTemplate.getRabbitTemplate());
		assertNotNull(amqpAdmin);
		assertEquals("localhost", connectionFactory.getHost());
		assertTrue("Listener container factory should be created by default",
				this.context.containsBean("rabbitListenerContainerFactory"));
	}

	@Test
	public void testRabbitTemplateWithOverrides() {
		load(TestConfiguration.class, "spring.rabbitmq.host:remote-server",
				"spring.rabbitmq.port:9000", "spring.rabbitmq.username:alice",
				"spring.rabbitmq.password:secret", "spring.rabbitmq.virtual_host:/vhost");
		CachingConnectionFactory connectionFactory = this.context
				.getBean(CachingConnectionFactory.class);
		assertEquals("remote-server", connectionFactory.getHost());
		assertEquals(9000, connectionFactory.getPort());
		assertEquals("/vhost", connectionFactory.getVirtualHost());
	}

	@Test
	public void testRabbitTemplateEmptyVirtualHost() {
		load(TestConfiguration.class, "spring.rabbitmq.virtual_host:");
		CachingConnectionFactory connectionFactory = this.context
				.getBean(CachingConnectionFactory.class);
		assertEquals("/", connectionFactory.getVirtualHost());
	}

	@Test
	public void testRabbitTemplateVirtualHostNoLeadingSlash() {
		load(TestConfiguration.class, "spring.rabbitmq.virtual_host:foo");
		CachingConnectionFactory connectionFactory = this.context
				.getBean(CachingConnectionFactory.class);
		assertEquals("foo", connectionFactory.getVirtualHost());
	}

	@Test
	public void testRabbitTemplateVirtualHostMultiLeadingSlashes() {
		load(TestConfiguration.class, "spring.rabbitmq.virtual_host:///foo");
		CachingConnectionFactory connectionFactory = this.context
				.getBean(CachingConnectionFactory.class);
		assertEquals("///foo", connectionFactory.getVirtualHost());
	}

	@Test
	public void testRabbitTemplateDefaultVirtualHost() {
		load(TestConfiguration.class, "spring.rabbitmq.virtual_host:/");
		CachingConnectionFactory connectionFactory = this.context
				.getBean(CachingConnectionFactory.class);
		assertEquals("/", connectionFactory.getVirtualHost());
	}

	@Test
	public void testConnectionFactoryBackOff() {
		load(TestConfiguration2.class);
		RabbitTemplate rabbitTemplate = this.context.getBean(RabbitTemplate.class);
		CachingConnectionFactory connectionFactory = this.context
				.getBean(CachingConnectionFactory.class);
		assertEquals(rabbitTemplate.getConnectionFactory(), connectionFactory);
		assertEquals("otherserver", connectionFactory.getHost());
		assertEquals(8001, connectionFactory.getPort());
	}

	@Test
	public void testRabbitTemplateBackOff() {
		load(TestConfiguration3.class);
		RabbitTemplate rabbitTemplate = this.context.getBean(RabbitTemplate.class);
		assertEquals(this.context.getBean("testMessageConverter"),
				rabbitTemplate.getMessageConverter());
	}

	@Test
	public void testRabbitMessagingTemplateBackOff() {
		load(TestConfiguration4.class);
		RabbitMessagingTemplate messagingTemplate = this.context
				.getBean(RabbitMessagingTemplate.class);
		assertEquals("fooBar", messagingTemplate.getDefaultDestination());
	}

	@Test
	public void testStaticQueues() {
		load(TestConfiguration.class, "spring.rabbitmq.dynamic:false");
		// There should NOT be an AmqpAdmin bean when dynamic is switch to false
		this.thrown.expect(NoSuchBeanDefinitionException.class);
		this.thrown.expectMessage("No qualifying bean of type "
				+ "[org.springframework.amqp.core.AmqpAdmin] is defined");
		this.context.getBean(AmqpAdmin.class);
	}

	@Test
	public void testEnableRabbitCreateDefaultContainerFactory() {
		load(EnableRabbitConfiguration.class);
		RabbitListenerContainerFactory<?> rabbitListenerContainerFactory = this.context
				.getBean("rabbitListenerContainerFactory",
						RabbitListenerContainerFactory.class);
		assertEquals(SimpleRabbitListenerContainerFactory.class,
				rabbitListenerContainerFactory.getClass());
	}

	@Test
	public void testRabbitListenerContainerFactoryBackOff() {
		load(TestConfiguration5.class);
		SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory = this.context
				.getBean("rabbitListenerContainerFactory",
						SimpleRabbitListenerContainerFactory.class);
		rabbitListenerContainerFactory.setTxSize(10);
		verify(rabbitListenerContainerFactory).setTxSize(10);
	}

	@Test
	public void enableRabbitAutomatically() throws Exception {
		load(NoEnableRabbitConfiguration.class);
		AnnotationConfigApplicationContext ctx = this.context;
		ctx.getBean(RabbitListenerConfigUtils.RABBIT_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME);
		ctx.getBean(RabbitListenerConfigUtils.RABBIT_LISTENER_ENDPOINT_REGISTRY_BEAN_NAME);
	}

	private void load(Class<?> config, String... environment) {
		this.context = doLoad(new Class<?>[] { config }, environment);
	}

	private AnnotationConfigApplicationContext doLoad(Class<?>[] configs,
			String... environment) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		applicationContext.register(configs);
		applicationContext.register(RabbitAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(applicationContext, environment);
		applicationContext.refresh();
		return applicationContext;
	}

	@Configuration
	protected static class TestConfiguration {

	}

	@Configuration
	protected static class TestConfiguration2 {
		@Bean
		ConnectionFactory aDifferentConnectionFactory() {
			return new CachingConnectionFactory("otherserver", 8001);
		}
	}

	@Configuration
	protected static class TestConfiguration3 {

		@Bean
		RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
			RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
			rabbitTemplate.setMessageConverter(testMessageConverter());
			return rabbitTemplate;
		}

		@Bean
		public MessageConverter testMessageConverter() {
			return mock(MessageConverter.class);
		}

	}

	@Configuration
	protected static class TestConfiguration4 {

		@Bean
		RabbitMessagingTemplate messagingTemplate(RabbitTemplate rabbitTemplate) {
			RabbitMessagingTemplate messagingTemplate = new RabbitMessagingTemplate(
					rabbitTemplate);
			messagingTemplate.setDefaultDestination("fooBar");
			return messagingTemplate;
		}
	}

	@Configuration
	protected static class TestConfiguration5 {

		@Bean
		RabbitListenerContainerFactory<?> rabbitListenerContainerFactory() {
			return mock(SimpleRabbitListenerContainerFactory.class);
		}

	}

	@Configuration
	@EnableRabbit
	protected static class EnableRabbitConfiguration {
	}

	@Configuration
	protected static class NoEnableRabbitConfiguration {
	}

}
