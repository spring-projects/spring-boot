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
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerConfigUtils;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerEndpoint;
import org.springframework.jms.config.SimpleJmsListenerContainerFactory;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JmsAutoConfiguration}.
 *
 * @author Greg Turnquist
 */
public class JmsAutoConfigurationTests {

	private static final String ACTIVEMQ_EMBEDDED_URL = "vm://localhost?broker.persistent=false";

	private static final String ACTIVEMQ_NETWORK_URL = "tcp://localhost:61616";

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testDefaultJmsConfiguration() {
		load(TestConfiguration.class);
		ActiveMQConnectionFactory connectionFactory = this.context
				.getBean(ActiveMQConnectionFactory.class);
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		JmsMessagingTemplate messagingTemplate = this.context
				.getBean(JmsMessagingTemplate.class);
		assertEquals(jmsTemplate.getConnectionFactory(), connectionFactory);
		assertEquals(jmsTemplate, messagingTemplate.getJmsTemplate());
		assertEquals(ACTIVEMQ_EMBEDDED_URL,
				((ActiveMQConnectionFactory) jmsTemplate.getConnectionFactory())
						.getBrokerURL());
		assertTrue("listener container factory should be created by default",
				this.context.containsBean("jmsListenerContainerFactory"));
	}

	@Test
	public void testConnectionFactoryBackOff() {
		load(TestConfiguration2.class);
		assertEquals("foobar", this.context.getBean(ActiveMQConnectionFactory.class)
				.getBrokerURL());
	}

	@Test
	public void testJmsTemplateBackOff() {
		load(TestConfiguration3.class);
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		assertEquals(999, jmsTemplate.getPriority());
	}

	@Test
	public void testJmsMessagingTemplateBackOff() {
		load(TestConfiguration5.class);
		JmsMessagingTemplate messagingTemplate = this.context
				.getBean(JmsMessagingTemplate.class);
		assertEquals("fooBar", messagingTemplate.getDefaultDestinationName());
	}

	@Test
	public void testJmsTemplateBackOffEverything() {
		this.context = createContext(TestConfiguration2.class, TestConfiguration3.class,
				TestConfiguration5.class);
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		assertEquals(999, jmsTemplate.getPriority());
		assertEquals("foobar", this.context.getBean(ActiveMQConnectionFactory.class)
				.getBrokerURL());
		JmsMessagingTemplate messagingTemplate = this.context
				.getBean(JmsMessagingTemplate.class);
		assertEquals("fooBar", messagingTemplate.getDefaultDestinationName());
		assertEquals(jmsTemplate, messagingTemplate.getJmsTemplate());
	}

	@Test
	public void testEnableJmsCreateDefaultContainerFactory() {
		load(EnableJmsConfiguration.class);
		JmsListenerContainerFactory<?> jmsListenerContainerFactory = this.context
				.getBean("jmsListenerContainerFactory", JmsListenerContainerFactory.class);
		assertEquals(DefaultJmsListenerContainerFactory.class,
				jmsListenerContainerFactory.getClass());
	}

	@Test
	public void testJmsListenerContainerFactoryBackOff() {
		this.context = createContext(TestConfiguration6.class,
				EnableJmsConfiguration.class);
		JmsListenerContainerFactory<?> jmsListenerContainerFactory = this.context
				.getBean("jmsListenerContainerFactory", JmsListenerContainerFactory.class);
		assertEquals(SimpleJmsListenerContainerFactory.class,
				jmsListenerContainerFactory.getClass());
	}

	@Test
	public void testPubSubDisabledByDefault() {
		load(TestConfiguration.class);
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		assertFalse(jmsTemplate.isPubSubDomain());
	}

	@Test
	public void testJmsTemplatePostProcessedSoThatPubSubIsTrue() {
		load(TestConfiguration4.class);
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		assertTrue(jmsTemplate.isPubSubDomain());
	}

	@Test
	public void testPubSubDomainActive() {
		load(TestConfiguration.class, "spring.jms.pubSubDomain:true");
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		DefaultMessageListenerContainer defaultMessageListenerContainer = this.context
				.getBean(DefaultJmsListenerContainerFactory.class)
				.createListenerContainer(mock(JmsListenerEndpoint.class));
		assertTrue(jmsTemplate.isPubSubDomain());
		assertTrue(defaultMessageListenerContainer.isPubSubDomain());
	}

	@Test
	public void testPubSubDomainOverride() {
		load(TestConfiguration.class, "spring.jms.pubSubDomain:false");
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
		load(TestConfiguration.class, "spring.activemq.inMemory:false");
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		ActiveMQConnectionFactory connectionFactory = this.context
				.getBean(ActiveMQConnectionFactory.class);
		assertNotNull(jmsTemplate);
		assertNotNull(connectionFactory);
		assertEquals(jmsTemplate.getConnectionFactory(), connectionFactory);
		assertEquals(ACTIVEMQ_NETWORK_URL,
				((ActiveMQConnectionFactory) jmsTemplate.getConnectionFactory())
						.getBrokerURL());
	}

	@Test
	public void testActiveMQOverriddenRemoteHost() {
		load(TestConfiguration.class, "spring.activemq.brokerUrl:tcp://remote-host:10000");
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
		load(TestConfiguration.class, "spring.activemq.pooled:true");
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		PooledConnectionFactory pool = this.context
				.getBean(PooledConnectionFactory.class);
		assertNotNull(jmsTemplate);
		assertNotNull(pool);
		assertEquals(jmsTemplate.getConnectionFactory(), pool);
		ActiveMQConnectionFactory factory = (ActiveMQConnectionFactory) pool
				.getConnectionFactory();
		assertEquals(ACTIVEMQ_EMBEDDED_URL, factory.getBrokerURL());
	}

	@Test
	public void testActiveMQOverriddenPoolAndStandalone() {
		load(TestConfiguration.class, "spring.activemq.pooled:true",
				"spring.activemq.inMemory:false");
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		PooledConnectionFactory pool = this.context
				.getBean(PooledConnectionFactory.class);
		assertNotNull(jmsTemplate);
		assertNotNull(pool);
		assertEquals(jmsTemplate.getConnectionFactory(), pool);
		ActiveMQConnectionFactory factory = (ActiveMQConnectionFactory) pool
				.getConnectionFactory();
		assertEquals(ACTIVEMQ_NETWORK_URL, factory.getBrokerURL());
	}

	@Test
	public void testActiveMQOverriddenPoolAndRemoteServer() {
		load(TestConfiguration.class, "spring.activemq.pooled:true",
				"spring.activemq.brokerUrl:tcp://remote-host:10000");
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

	@Test
	public void enableJmsAutomatically() throws Exception {
		load(NoEnableJmsConfiguration.class);
		AnnotationConfigApplicationContext ctx = this.context;
		ctx.getBean(JmsListenerConfigUtils.JMS_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME);
		ctx.getBean(JmsListenerConfigUtils.JMS_LISTENER_ENDPOINT_REGISTRY_BEAN_NAME);
	}

	private AnnotationConfigApplicationContext createContext(
			Class<?>... additionalClasses) {
		return doLoad(additionalClasses);
	}

	private void load(Class<?> config, String... environment) {
		this.context = doLoad(new Class<?>[] { config }, environment);
	}

	private AnnotationConfigApplicationContext doLoad(Class<?>[] configs,
			String... environment) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		applicationContext.register(configs);
		applicationContext.register(ActiveMQAutoConfiguration.class,
				JmsAutoConfiguration.class);
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

	@Configuration
	protected static class TestConfiguration5 {

		@Bean
		JmsMessagingTemplate jmsMessagingTemplate(JmsTemplate jmsTemplate) {
			JmsMessagingTemplate messagingTemplate = new JmsMessagingTemplate(jmsTemplate);
			messagingTemplate.setDefaultDestinationName("fooBar");
			return messagingTemplate;
		}

	}

	@Configuration
	protected static class TestConfiguration6 {

		@Bean
		JmsListenerContainerFactory<?> jmsListenerContainerFactory(
				ConnectionFactory connectionFactory) {
			SimpleJmsListenerContainerFactory factory = new SimpleJmsListenerContainerFactory();
			factory.setConnectionFactory(connectionFactory);
			return factory;
		}

	}

	@Configuration
	@EnableJms
	protected static class EnableJmsConfiguration {
	}

	@Configuration
	protected static class NoEnableJmsConfiguration {
	}

}
