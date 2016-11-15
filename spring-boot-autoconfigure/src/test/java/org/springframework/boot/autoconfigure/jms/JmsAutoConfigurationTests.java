/*
 * Copyright 2012-2016 the original author or authors.
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
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.junit.After;
import org.junit.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerConfigUtils;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerEndpoint;
import org.springframework.jms.config.SimpleJmsListenerContainerFactory;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.transaction.jta.JtaTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JmsAutoConfiguration}.
 *
 * @author Greg Turnquist
 * @author Stephane Nicoll
 * @author Aurélien Leboulanger
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
		assertThat(connectionFactory).isEqualTo(jmsTemplate.getConnectionFactory());
		assertThat(messagingTemplate.getJmsTemplate()).isEqualTo(jmsTemplate);
		assertThat(((ActiveMQConnectionFactory) jmsTemplate.getConnectionFactory())
				.getBrokerURL()).isEqualTo(ACTIVEMQ_EMBEDDED_URL);
		assertThat(this.context.containsBean("jmsListenerContainerFactory")).isTrue();
	}

	@Test
	public void testConnectionFactoryBackOff() {
		load(TestConfiguration2.class);
		assertThat(this.context.getBean(ActiveMQConnectionFactory.class).getBrokerURL())
				.isEqualTo("foobar");
	}

	@Test
	public void testJmsTemplateBackOff() {
		load(TestConfiguration3.class);
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		assertThat(jmsTemplate.getPriority()).isEqualTo(999);
	}

	@Test
	public void testJmsMessagingTemplateBackOff() {
		load(TestConfiguration5.class);
		JmsMessagingTemplate messagingTemplate = this.context
				.getBean(JmsMessagingTemplate.class);
		assertThat(messagingTemplate.getDefaultDestinationName()).isEqualTo("fooBar");
	}

	@Test
	public void testJmsTemplateBackOffEverything() {
		this.context = createContext(TestConfiguration2.class, TestConfiguration3.class,
				TestConfiguration5.class);
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		assertThat(jmsTemplate.getPriority()).isEqualTo(999);
		assertThat(this.context.getBean(ActiveMQConnectionFactory.class).getBrokerURL())
				.isEqualTo("foobar");
		JmsMessagingTemplate messagingTemplate = this.context
				.getBean(JmsMessagingTemplate.class);
		assertThat(messagingTemplate.getDefaultDestinationName()).isEqualTo("fooBar");
		assertThat(messagingTemplate.getJmsTemplate()).isEqualTo(jmsTemplate);
	}

	@Test
	public void testEnableJmsCreateDefaultContainerFactory() {
		load(EnableJmsConfiguration.class);
		JmsListenerContainerFactory<?> jmsListenerContainerFactory = this.context.getBean(
				"jmsListenerContainerFactory", JmsListenerContainerFactory.class);
		assertThat(jmsListenerContainerFactory.getClass())
				.isEqualTo(DefaultJmsListenerContainerFactory.class);
	}

	@Test
	public void testJmsListenerContainerFactoryBackOff() {
		this.context = createContext(TestConfiguration6.class,
				EnableJmsConfiguration.class);
		JmsListenerContainerFactory<?> jmsListenerContainerFactory = this.context.getBean(
				"jmsListenerContainerFactory", JmsListenerContainerFactory.class);
		assertThat(jmsListenerContainerFactory.getClass())
				.isEqualTo(SimpleJmsListenerContainerFactory.class);
	}

	@Test
	public void testJmsListenerContainerFactoryWithCustomSettings() {
		load(EnableJmsConfiguration.class, "spring.jms.listener.autoStartup=false",
				"spring.jms.listener.acknowledgeMode=client",
				"spring.jms.listener.concurrency=2",
				"spring.jms.listener.maxConcurrency=10");
		JmsListenerContainerFactory<?> jmsListenerContainerFactory = this.context.getBean(
				"jmsListenerContainerFactory", JmsListenerContainerFactory.class);
		assertThat(jmsListenerContainerFactory.getClass())
				.isEqualTo(DefaultJmsListenerContainerFactory.class);
		DefaultMessageListenerContainer listenerContainer = ((DefaultJmsListenerContainerFactory) jmsListenerContainerFactory)
				.createListenerContainer(mock(JmsListenerEndpoint.class));
		assertThat(listenerContainer.isAutoStartup()).isFalse();
		assertThat(listenerContainer.getSessionAcknowledgeMode())
				.isEqualTo(Session.CLIENT_ACKNOWLEDGE);
		assertThat(listenerContainer.getConcurrentConsumers()).isEqualTo(2);
		assertThat(listenerContainer.getMaxConcurrentConsumers()).isEqualTo(10);
	}

	@Test
	public void testDefaultContainerFactoryWithJtaTransactionManager() {
		this.context = createContext(TestConfiguration7.class,
				EnableJmsConfiguration.class);
		JmsListenerContainerFactory<?> jmsListenerContainerFactory = this.context.getBean(
				"jmsListenerContainerFactory", JmsListenerContainerFactory.class);
		assertThat(jmsListenerContainerFactory.getClass())
				.isEqualTo(DefaultJmsListenerContainerFactory.class);
		DefaultMessageListenerContainer listenerContainer = ((DefaultJmsListenerContainerFactory) jmsListenerContainerFactory)
				.createListenerContainer(mock(JmsListenerEndpoint.class));
		assertThat(listenerContainer.isSessionTransacted()).isFalse();
		assertThat(new DirectFieldAccessor(listenerContainer)
				.getPropertyValue("transactionManager"))
						.isSameAs(this.context.getBean(JtaTransactionManager.class));
	}

	@Test
	public void testDefaultContainerFactoryNonJtaTransactionManager() {
		this.context = createContext(TestConfiguration8.class,
				EnableJmsConfiguration.class);
		JmsListenerContainerFactory<?> jmsListenerContainerFactory = this.context.getBean(
				"jmsListenerContainerFactory", JmsListenerContainerFactory.class);
		assertThat(jmsListenerContainerFactory.getClass())
				.isEqualTo(DefaultJmsListenerContainerFactory.class);
		DefaultMessageListenerContainer listenerContainer = ((DefaultJmsListenerContainerFactory) jmsListenerContainerFactory)
				.createListenerContainer(mock(JmsListenerEndpoint.class));
		assertThat(listenerContainer.isSessionTransacted()).isTrue();
		assertThat(new DirectFieldAccessor(listenerContainer)
				.getPropertyValue("transactionManager")).isNull();
	}

	@Test
	public void testDefaultContainerFactoryNoTransactionManager() {
		this.context = createContext(EnableJmsConfiguration.class);
		JmsListenerContainerFactory<?> jmsListenerContainerFactory = this.context.getBean(
				"jmsListenerContainerFactory", JmsListenerContainerFactory.class);
		assertThat(jmsListenerContainerFactory.getClass())
				.isEqualTo(DefaultJmsListenerContainerFactory.class);
		DefaultMessageListenerContainer listenerContainer = ((DefaultJmsListenerContainerFactory) jmsListenerContainerFactory)
				.createListenerContainer(mock(JmsListenerEndpoint.class));
		assertThat(listenerContainer.isSessionTransacted()).isTrue();
		assertThat(new DirectFieldAccessor(listenerContainer)
				.getPropertyValue("transactionManager")).isNull();
	}

	@Test
	public void testDefaultContainerFactoryWithMessageConverters() {
		this.context = createContext(MessageConvertersConfiguration.class,
				EnableJmsConfiguration.class);
		JmsListenerContainerFactory<?> jmsListenerContainerFactory = this.context.getBean(
				"jmsListenerContainerFactory", JmsListenerContainerFactory.class);
		assertThat(jmsListenerContainerFactory.getClass())
				.isEqualTo(DefaultJmsListenerContainerFactory.class);
		DefaultMessageListenerContainer listenerContainer = ((DefaultJmsListenerContainerFactory) jmsListenerContainerFactory)
				.createListenerContainer(mock(JmsListenerEndpoint.class));
		assertThat(listenerContainer.getMessageConverter())
				.isSameAs(this.context.getBean("myMessageConverter"));
	}

	@Test
	public void testCustomContainerFactoryWithConfigurer() {
		this.context = doLoad(
				new Class<?>[] { TestConfiguration9.class, EnableJmsConfiguration.class },
				"spring.jms.listener.autoStartup=false");
		assertThat(this.context.containsBean("jmsListenerContainerFactory")).isTrue();
		JmsListenerContainerFactory<?> jmsListenerContainerFactory = this.context.getBean(
				"customListenerContainerFactory", JmsListenerContainerFactory.class);
		assertThat(jmsListenerContainerFactory)
				.isInstanceOf(DefaultJmsListenerContainerFactory.class);
		DefaultMessageListenerContainer listenerContainer = ((DefaultJmsListenerContainerFactory) jmsListenerContainerFactory)
				.createListenerContainer(mock(JmsListenerEndpoint.class));
		assertThat(listenerContainer.getCacheLevel())
				.isEqualTo(DefaultMessageListenerContainer.CACHE_CONSUMER);
		assertThat(listenerContainer.isAutoStartup()).isFalse();
	}

	@Test
	public void testJmsTemplateWithMessageConverter() {
		load(MessageConvertersConfiguration.class);
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		assertThat(jmsTemplate.getMessageConverter())
				.isSameAs(this.context.getBean("myMessageConverter"));
	}

	@Test
	public void testJmsTemplateWithDestinationResolver() {
		load(DestinationResolversConfiguration.class);
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		assertThat(jmsTemplate.getDestinationResolver())
				.isSameAs(this.context.getBean("myDestinationResolver"));
	}

	@Test
	public void testJmsTemplateFullCustomization() {
		load(MessageConvertersConfiguration.class,
				"spring.jms.template.default-destination=testQueue",
				"spring.jms.template.delivery-delay=500",
				"spring.jms.template.delivery-mode=non-persistent",
				"spring.jms.template.priority=6", "spring.jms.template.time-to-live=6000",
				"spring.jms.template.receive-timeout=2000");
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		assertThat(jmsTemplate.getMessageConverter())
				.isSameAs(this.context.getBean("myMessageConverter"));
		assertThat(jmsTemplate.isPubSubDomain()).isFalse();
		assertThat(jmsTemplate.getDefaultDestinationName()).isEqualTo("testQueue");
		assertThat(jmsTemplate.getDeliveryDelay()).isEqualTo(500);
		assertThat(jmsTemplate.getDeliveryMode()).isEqualTo(1);
		assertThat(jmsTemplate.getPriority()).isEqualTo(6);
		assertThat(jmsTemplate.getTimeToLive()).isEqualTo(6000);
		assertThat(jmsTemplate.isExplicitQosEnabled()).isTrue();
		assertThat(jmsTemplate.getReceiveTimeout()).isEqualTo(2000);
	}

	@Test
	public void testPubSubDisabledByDefault() {
		load(TestConfiguration.class);
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		assertThat(jmsTemplate.isPubSubDomain()).isFalse();
	}

	@Test
	public void testJmsTemplatePostProcessedSoThatPubSubIsTrue() {
		load(TestConfiguration4.class);
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		assertThat(jmsTemplate.isPubSubDomain()).isTrue();
	}

	@Test
	public void testPubSubDomainActive() {
		load(TestConfiguration.class, "spring.jms.pubSubDomain:true");
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		DefaultMessageListenerContainer defaultMessageListenerContainer = this.context
				.getBean(DefaultJmsListenerContainerFactory.class)
				.createListenerContainer(mock(JmsListenerEndpoint.class));
		assertThat(jmsTemplate.isPubSubDomain()).isTrue();
		assertThat(defaultMessageListenerContainer.isPubSubDomain()).isTrue();
	}

	@Test
	public void testPubSubDomainOverride() {
		load(TestConfiguration.class, "spring.jms.pubSubDomain:false");
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		ActiveMQConnectionFactory connectionFactory = this.context
				.getBean(ActiveMQConnectionFactory.class);
		assertThat(jmsTemplate).isNotNull();
		assertThat(jmsTemplate.isPubSubDomain()).isFalse();
		assertThat(connectionFactory).isNotNull();
		assertThat(connectionFactory).isEqualTo(jmsTemplate.getConnectionFactory());
	}

	@Test
	public void testActiveMQOverriddenStandalone() {
		load(TestConfiguration.class, "spring.activemq.inMemory:false");
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		ActiveMQConnectionFactory connectionFactory = this.context
				.getBean(ActiveMQConnectionFactory.class);
		assertThat(jmsTemplate).isNotNull();
		assertThat(connectionFactory).isNotNull();
		assertThat(connectionFactory).isEqualTo(jmsTemplate.getConnectionFactory());
		assertThat(((ActiveMQConnectionFactory) jmsTemplate.getConnectionFactory())
				.getBrokerURL()).isEqualTo(ACTIVEMQ_NETWORK_URL);
	}

	@Test
	public void testActiveMQOverriddenRemoteHost() {
		load(TestConfiguration.class,
				"spring.activemq.brokerUrl:tcp://remote-host:10000");
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		ActiveMQConnectionFactory connectionFactory = this.context
				.getBean(ActiveMQConnectionFactory.class);
		assertThat(jmsTemplate).isNotNull();
		assertThat(connectionFactory).isNotNull();
		assertThat(connectionFactory).isEqualTo(jmsTemplate.getConnectionFactory());
		assertThat(((ActiveMQConnectionFactory) jmsTemplate.getConnectionFactory())
				.getBrokerURL()).isEqualTo("tcp://remote-host:10000");
	}

	@Test
	public void testActiveMQOverriddenPool() {
		load(TestConfiguration.class, "spring.activemq.pool.enabled:true");
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		PooledConnectionFactory pool = this.context
				.getBean(PooledConnectionFactory.class);
		assertThat(jmsTemplate).isNotNull();
		assertThat(pool).isNotNull();
		assertThat(pool).isEqualTo(jmsTemplate.getConnectionFactory());
		ActiveMQConnectionFactory factory = (ActiveMQConnectionFactory) pool
				.getConnectionFactory();
		assertThat(factory.getBrokerURL()).isEqualTo(ACTIVEMQ_EMBEDDED_URL);
	}

	@Test
	public void testActiveMQOverriddenPoolAndStandalone() {
		load(TestConfiguration.class, "spring.activemq.pool.enabled:true",
				"spring.activemq.inMemory:false");
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		PooledConnectionFactory pool = this.context
				.getBean(PooledConnectionFactory.class);
		assertThat(jmsTemplate).isNotNull();
		assertThat(pool).isNotNull();
		assertThat(pool).isEqualTo(jmsTemplate.getConnectionFactory());
		ActiveMQConnectionFactory factory = (ActiveMQConnectionFactory) pool
				.getConnectionFactory();
		assertThat(factory.getBrokerURL()).isEqualTo(ACTIVEMQ_NETWORK_URL);
	}

	@Test
	public void testActiveMQOverriddenPoolAndRemoteServer() {
		load(TestConfiguration.class, "spring.activemq.pool.enabled:true",
				"spring.activemq.brokerUrl:tcp://remote-host:10000");
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		PooledConnectionFactory pool = this.context
				.getBean(PooledConnectionFactory.class);
		assertThat(jmsTemplate).isNotNull();
		assertThat(pool).isNotNull();
		assertThat(pool).isEqualTo(jmsTemplate.getConnectionFactory());
		ActiveMQConnectionFactory factory = (ActiveMQConnectionFactory) pool
				.getConnectionFactory();
		assertThat(factory.getBrokerURL()).isEqualTo("tcp://remote-host:10000");
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
			JmsMessagingTemplate messagingTemplate = new JmsMessagingTemplate(
					jmsTemplate);
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
	protected static class TestConfiguration7 {

		@Bean
		JtaTransactionManager transactionManager() {
			return mock(JtaTransactionManager.class);
		}

	}

	@Configuration
	protected static class TestConfiguration8 {

		@Bean
		DataSourceTransactionManager transactionManager() {
			return mock(DataSourceTransactionManager.class);
		}

	}

	@Configuration
	protected static class MessageConvertersConfiguration {

		@Bean
		@Primary
		public MessageConverter myMessageConverter() {
			return mock(MessageConverter.class);
		}

		@Bean
		public MessageConverter anotherMessageConverter() {
			return mock(MessageConverter.class);
		}

	}

	@Configuration
	protected static class DestinationResolversConfiguration {

		@Bean
		@Primary
		public DestinationResolver myDestinationResolver() {
			return mock(DestinationResolver.class);
		}

		@Bean
		public DestinationResolver anotherDestinationResolver() {
			return mock(DestinationResolver.class);
		}

	}

	@Configuration
	protected static class TestConfiguration9 {

		@Bean
		JmsListenerContainerFactory<?> customListenerContainerFactory(
				DefaultJmsListenerContainerFactoryConfigurer configurer,
				ConnectionFactory connectionFactory) {
			DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
			configurer.configure(factory, connectionFactory);
			factory.setCacheLevel(DefaultMessageListenerContainer.CACHE_CONSUMER);
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
