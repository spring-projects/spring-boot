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

package org.springframework.boot.autoconfigure.jms;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.junit.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.EnvironmentTestUtils;
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
		this.context = createContext(TestConfiguration.class);
		this.context.refresh();
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		ActiveMQConnectionFactory connectionFactory = this.context
				.getBean(ActiveMQConnectionFactory.class);
		assertNotNull(jmsTemplate);
		assertNotNull(connectionFactory);
		assertEquals(jmsTemplate.getConnectionFactory(), connectionFactory);
		assertEquals(ActiveMQProperties.DEFAULT_EMBEDDED_BROKER_URL,
				((ActiveMQConnectionFactory) jmsTemplate.getConnectionFactory())
						.getBrokerURL());
	}

	@Test
	public void testConnectionFactoryBackoff() {
		this.context = createContext(TestConfiguration2.class);
		this.context.refresh();
		assertEquals("foobar", this.context.getBean(ActiveMQConnectionFactory.class)
				.getBrokerURL());
	}

	@Test
	public void testJmsTemplateBackoff() {
		this.context = createContext(TestConfiguration3.class);
		this.context.refresh();
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		assertEquals(999, jmsTemplate.getPriority());
	}

	@Test
	public void testJmsTemplateBackoffEverything() {
		this.context = createContext(TestConfiguration2.class, TestConfiguration3.class);
		this.context.refresh();
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		assertEquals(999, jmsTemplate.getPriority());
		assertEquals("foobar", this.context.getBean(ActiveMQConnectionFactory.class)
				.getBrokerURL());
	}

	@Test
	public void testPubSubDisabledByDefault() {
		this.context = createContext(TestConfiguration.class);
		this.context.refresh();
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		assertFalse(jmsTemplate.isPubSubDomain());
	}

	@Test
	public void testJmsTemplatePostProcessedSoThatPubSubIsTrue() {
		this.context = createContext(TestConfiguration4.class);
		this.context.refresh();
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		assertTrue(jmsTemplate.isPubSubDomain());
	}

	@Test
	public void testJmsTemplateOverridden() {
		this.context = createContext(TestConfiguration.class);
		EnvironmentTestUtils
				.addEnvironment(this.context, "spring.jms.pubSubDomain:false");
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
		this.context = createContext(TestConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.activemq.inMemory:false");
		this.context.refresh();
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		ActiveMQConnectionFactory connectionFactory = this.context
				.getBean(ActiveMQConnectionFactory.class);
		assertNotNull(jmsTemplate);
		assertNotNull(connectionFactory);
		assertEquals(jmsTemplate.getConnectionFactory(), connectionFactory);
		assertEquals(ActiveMQProperties.DEFAULT_NETWORK_BROKER_URL,
				((ActiveMQConnectionFactory) jmsTemplate.getConnectionFactory())
						.getBrokerURL());
	}

	@Test
	public void testActiveMQOverriddenRemoteHost() {
		this.context = createContext(TestConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.activemq.brokerUrl:tcp://remote-host:10000");
		this.context.refresh();
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		ActiveMQConnectionFactory connectionFactory = this.context
				.getBean(ActiveMQConnectionFactory.class);
		assertNotNull(jmsTemplate);
		assertNotNull(connectionFactory);
		assertEquals(jmsTemplate.getConnectionFactory(), connectionFactory);
		assertEquals("tcp://remote-host:10000",
				((ActiveMQConnectionFactory) jmsTemplate.getConnectionFactory())
						.getBrokerURL());
	}

	@Test
	public void testActiveMQOverriddenPool() {
		this.context = createContext(TestConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context, "spring.activemq.pooled:true");
		this.context.refresh();
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		PooledConnectionFactory pool = this.context
				.getBean(PooledConnectionFactory.class);
		assertNotNull(jmsTemplate);
		assertNotNull(pool);
		assertEquals(jmsTemplate.getConnectionFactory(), pool);
		ActiveMQConnectionFactory factory = (ActiveMQConnectionFactory) pool
				.getConnectionFactory();
		assertEquals(ActiveMQProperties.DEFAULT_EMBEDDED_BROKER_URL,
				factory.getBrokerURL());
	}

	@Test
	public void testActiveMQOverriddenPoolAndStandalone() {
		this.context = createContext(TestConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context, "spring.activemq.pooled:true",
				"spring.activemq.inMemory:false");
		this.context.refresh();
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		PooledConnectionFactory pool = this.context
				.getBean(PooledConnectionFactory.class);
		assertNotNull(jmsTemplate);
		assertNotNull(pool);
		assertEquals(jmsTemplate.getConnectionFactory(), pool);
		ActiveMQConnectionFactory factory = (ActiveMQConnectionFactory) pool
				.getConnectionFactory();
		assertEquals(ActiveMQProperties.DEFAULT_NETWORK_BROKER_URL,
				factory.getBrokerURL());
	}

	@Test
	public void testActiveMQOverriddenPoolAndRemoteServer() {
		this.context = createContext(TestConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context, "spring.activemq.pooled:true",
				"spring.activemq.brokerUrl:tcp://remote-host:10000");
		this.context.refresh();
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		PooledConnectionFactory pool = this.context
				.getBean(PooledConnectionFactory.class);
		assertNotNull(jmsTemplate);
		assertNotNull(pool);
		assertEquals(jmsTemplate.getConnectionFactory(), pool);
		ActiveMQConnectionFactory factory = (ActiveMQConnectionFactory) pool
				.getConnectionFactory();
		assertEquals("tcp://remote-host:10000", factory.getBrokerURL());
	}

	private AnnotationConfigApplicationContext createContext(
			Class<?>... additionalClasses) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(additionalClasses);
		context.register(ActiveMQAutoConfiguration.class,
				JmsTemplateAutoConfiguration.class);
		return context;
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
				jmsTemplate.setPubSubDomain(true);
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
