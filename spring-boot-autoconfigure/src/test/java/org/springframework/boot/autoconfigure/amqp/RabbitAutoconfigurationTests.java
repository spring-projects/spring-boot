/*
 * Copyright 2012-2013 the original author or authors.
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
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.TestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests for {@link RabbitAutoConfiguration}.
 * 
 * @author Greg Turnquist
 */
public class RabbitAutoconfigurationTests {

	private AnnotationConfigApplicationContext context;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testDefaultRabbitTemplate() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfiguration.class, RabbitAutoConfiguration.class);
		this.context.refresh();
		RabbitTemplate rabbitTemplate = this.context.getBean(RabbitTemplate.class);
		CachingConnectionFactory connectionFactory = this.context
				.getBean(CachingConnectionFactory.class);
		RabbitAdmin amqpAdmin = this.context.getBean(RabbitAdmin.class);
		assertNotNull(rabbitTemplate);
		assertNotNull(connectionFactory);
		assertNotNull(amqpAdmin);
		assertEquals(rabbitTemplate.getConnectionFactory(), connectionFactory);
		assertEquals(connectionFactory.getHost(), "localhost");
	}

	@Test
	public void testRabbitTemplateWithOverrides() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfiguration.class, RabbitAutoConfiguration.class);
		TestUtils.addEnviroment(this.context, "spring.rabbitmq.host:remote-server",
				"spring.rabbitmq.port:9000", "spring.rabbitmq.username:alice",
				"spring.rabbitmq.password:secret", "spring.rabbitmq.virtual_host:/vhost");
		this.context.refresh();
		CachingConnectionFactory connectionFactory = this.context
				.getBean(CachingConnectionFactory.class);
		assertEquals(connectionFactory.getHost(), "remote-server");
		assertEquals(connectionFactory.getPort(), 9000);
		assertEquals(connectionFactory.getVirtualHost(), "/vhost");
	}

	@Test
	public void testRabbitTemplateEmptyVirtualHost() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfiguration.class, RabbitAutoConfiguration.class);
		TestUtils.addEnviroment(this.context, "spring.rabbitmq.virtual_host:");
		this.context.refresh();
		CachingConnectionFactory connectionFactory = this.context
				.getBean(CachingConnectionFactory.class);
		assertEquals(connectionFactory.getVirtualHost(), "/");
	}

	@Test
	public void testRabbitTemplateVirtualHostMissingSlash() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfiguration.class, RabbitAutoConfiguration.class);
		TestUtils.addEnviroment(this.context, "spring.rabbitmq.virtual_host:foo");
		this.context.refresh();
		CachingConnectionFactory connectionFactory = this.context
				.getBean(CachingConnectionFactory.class);
		assertEquals(connectionFactory.getVirtualHost(), "/foo");
	}

	@Test
	public void testRabbitTemplateDefaultVirtualHost() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfiguration.class, RabbitAutoConfiguration.class);
		TestUtils.addEnviroment(this.context, "spring.rabbitmq.virtual_host:/");
		this.context.refresh();
		CachingConnectionFactory connectionFactory = this.context
				.getBean(CachingConnectionFactory.class);
		assertEquals(connectionFactory.getVirtualHost(), "/");
	}

	@Test
	public void testConnectionFactoryBackoff() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfiguration2.class, RabbitAutoConfiguration.class);
		this.context.refresh();
		RabbitTemplate rabbitTemplate = this.context.getBean(RabbitTemplate.class);
		CachingConnectionFactory connectionFactory = this.context
				.getBean(CachingConnectionFactory.class);
		assertEquals(rabbitTemplate.getConnectionFactory(), connectionFactory);
		assertEquals(connectionFactory.getHost(), "otherserver");
		assertEquals(connectionFactory.getPort(), 8001);
	}

	@Test
	public void testStaticQueues() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfiguration.class, RabbitAutoConfiguration.class);
		TestUtils.addEnviroment(this.context, "spring.rabbitmq.dynamic:false");
		this.context.refresh();
		// There should NOT be an AmqpAdmin bean when dynamic is switch to false
		this.thrown.expect(NoSuchBeanDefinitionException.class);
		this.thrown.expectMessage("No qualifying bean of type "
				+ "[org.springframework.amqp.core.AmqpAdmin] is defined");
		this.context.getBean(AmqpAdmin.class);
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
}
