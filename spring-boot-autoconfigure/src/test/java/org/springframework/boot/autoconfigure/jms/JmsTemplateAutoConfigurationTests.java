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

package org.springframework.boot.autoconfigure.jms;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.junit.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.TestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link JmsTemplateAutoConfiguration}.
 * 
 * @author Greg Turnquist
 */
public class JmsTemplateAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@Test
	public void testDefaultJmsTemplate() {
		this.context = new AnnotationConfigApplicationContext();
		this.context
				.register(TestConfiguration.class, JmsTemplateAutoConfiguration.class);
		this.context.refresh();
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		ActiveMQConnectionFactory connectionFactory = this.context
				.getBean(ActiveMQConnectionFactory.class);
		assertNotNull(jmsTemplate);
		assertNotNull(connectionFactory);
		assertEquals(jmsTemplate.getConnectionFactory(), connectionFactory);
		assertEquals(((ActiveMQConnectionFactory)jmsTemplate.getConnectionFactory()).getBrokerURL(), "vm://localhost");
	}

	@Test
	public void testConnectionFactoryBackoff() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfiguration2.class,
				JmsTemplateAutoConfiguration.class);
		this.context.refresh();
		assertEquals("foobar", this.context.getBean(ActiveMQConnectionFactory.class)
				.getBrokerURL());
	}

	@Test
	public void testJmsTemplateBackoff() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfiguration3.class,
				JmsTemplateAutoConfiguration.class);
		this.context.refresh();
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		assertEquals(999, jmsTemplate.getPriority());
	}

	@Test
	public void testJmsTemplateBackoffEverything() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfiguration2.class, TestConfiguration3.class,
				JmsTemplateAutoConfiguration.class);
		this.context.refresh();
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		assertEquals(999, jmsTemplate.getPriority());
		assertEquals("foobar", this.context.getBean(ActiveMQConnectionFactory.class)
				.getBrokerURL());
	}

	@Test
	public void testPubSubEnabledByDefault() {
		this.context = new AnnotationConfigApplicationContext();
		this.context
				.register(TestConfiguration.class, JmsTemplateAutoConfiguration.class);
		this.context.refresh();
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		assertTrue(jmsTemplate.isPubSubDomain());
	}

	@Test
	public void testJmsTemplatePostProcessedSoThatPubSubIsFalse() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfiguration4.class,
				JmsTemplateAutoConfiguration.class);
		this.context.refresh();
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		assertFalse(jmsTemplate.isPubSubDomain());
	}
	
	@Test
	public void testJmsTemplateOverridden() {
		this.context = new AnnotationConfigApplicationContext();
		this.context
				.register(TestConfiguration.class, JmsTemplateAutoConfiguration.class);
		TestUtils.addEnviroment(this.context, "spring.jms.pubSubDomain:false");
		this.context.refresh();
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		ActiveMQConnectionFactory connectionFactory = this.context
				.getBean(ActiveMQConnectionFactory.class);
		assertNotNull(jmsTemplate);
		assertFalse(jmsTemplate.isPubSubDomain());
		assertNotNull(connectionFactory);
		assertEquals(jmsTemplate.getConnectionFactory(), connectionFactory);
	}

	@Test
	public void testActiveMQOverriddenStandalone() {
		this.context = new AnnotationConfigApplicationContext();
		this.context
				.register(TestConfiguration.class, JmsTemplateAutoConfiguration.class);
		TestUtils.addEnviroment(this.context, "spring.activemq.inMemory:false");
		this.context.refresh();
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		ActiveMQConnectionFactory connectionFactory = this.context
				.getBean(ActiveMQConnectionFactory.class);
		assertNotNull(jmsTemplate);
		assertNotNull(connectionFactory);
		assertEquals(jmsTemplate.getConnectionFactory(), connectionFactory);
		assertEquals(((ActiveMQConnectionFactory)jmsTemplate.getConnectionFactory()).getBrokerURL(), 
				"tcp://localhost:61616");
	}

	@Test
	public void testActiveMQOverriddenRemoteHost() {
		this.context = new AnnotationConfigApplicationContext();
		this.context
				.register(TestConfiguration.class, JmsTemplateAutoConfiguration.class);
		TestUtils.addEnviroment(this.context, "spring.activemq.inMemory:false",
				"spring.activemq.brokerURL:tcp://remote-host:10000");
		this.context.refresh();
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		ActiveMQConnectionFactory connectionFactory = this.context
				.getBean(ActiveMQConnectionFactory.class);
		assertNotNull(jmsTemplate);
		assertNotNull(connectionFactory);
		assertEquals(jmsTemplate.getConnectionFactory(), connectionFactory);
		assertEquals(((ActiveMQConnectionFactory)jmsTemplate.getConnectionFactory()).getBrokerURL(), 
				"tcp://remote-host:10000");
	}

	@Test
	public void testActiveMQOverriddenPool() {
		this.context = new AnnotationConfigApplicationContext();
		this.context
				.register(TestConfiguration.class, JmsTemplateAutoConfiguration.class);
		TestUtils.addEnviroment(this.context, "spring.activemq.pooled:true");
		this.context.refresh();
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		PooledConnectionFactory pool = this.context
				.getBean(PooledConnectionFactory.class);
		assertNotNull(jmsTemplate);
		assertNotNull(pool);
		assertEquals(jmsTemplate.getConnectionFactory(), pool);
		ActiveMQConnectionFactory factory = (ActiveMQConnectionFactory) pool.getConnectionFactory();
		assertEquals("vm://localhost", factory.getBrokerURL());
	}

	@Test
	public void testActiveMQOverriddenPoolAndStandalone() {
		this.context = new AnnotationConfigApplicationContext();
		this.context
				.register(TestConfiguration.class, JmsTemplateAutoConfiguration.class);
		TestUtils.addEnviroment(this.context, "spring.activemq.pooled:true",
				"spring.activemq.inMemory:false");
		this.context.refresh();
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		PooledConnectionFactory pool = this.context
				.getBean(PooledConnectionFactory.class);
		assertNotNull(jmsTemplate);
		assertNotNull(pool);
		assertEquals(jmsTemplate.getConnectionFactory(), pool);
		ActiveMQConnectionFactory factory = (ActiveMQConnectionFactory) pool.getConnectionFactory();
		assertEquals("tcp://localhost:61616", factory.getBrokerURL());
	}

	@Test
	public void testActiveMQOverriddenPoolAndRemoteServer() {
		this.context = new AnnotationConfigApplicationContext();
		this.context
				.register(TestConfiguration.class, JmsTemplateAutoConfiguration.class);
		TestUtils.addEnviroment(this.context, "spring.activemq.pooled:true",
				"spring.activemq.inMemory:false", "spring.activemq.brokerURL:tcp://remote-host:10000");
		this.context.refresh();
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		PooledConnectionFactory pool = this.context
				.getBean(PooledConnectionFactory.class);
		assertNotNull(jmsTemplate);
		assertNotNull(pool);
		assertEquals(jmsTemplate.getConnectionFactory(), pool);
		ActiveMQConnectionFactory factory = (ActiveMQConnectionFactory) pool.getConnectionFactory();
		assertEquals("tcp://remote-host:10000", factory.getBrokerURL());
	}

	@Configuration
	protected static class TestConfiguration {
	}

	@Configuration
	protected static class TestConfiguration2 {
		@Bean
		ConnectionFactory connectionFactory() {
			return new ActiveMQConnectionFactory() {
				{
					setBrokerURL("foobar");
				}
			};
		}
	}

	@Configuration
	protected static class TestConfiguration3 {
		@Bean
		JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
			JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
			jmsTemplate.setPriority(999);
			return jmsTemplate;
		}

	}

	@Configuration
	protected static class TestConfiguration4 implements BeanPostProcessor {
		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName)
				throws BeansException {
			if (bean.getClass().isAssignableFrom(JmsTemplate.class)) {
				JmsTemplate jmsTemplate = (JmsTemplate) bean;
				jmsTemplate.setPubSubDomain(false);
			}
			return bean;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName)
				throws BeansException {
			return bean;
		}
	}
}
