/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.jms;

import javax.jms.ConnectionFactory;
import javax.jms.ExceptionListener;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.jupiter.api.Test;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
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
import org.springframework.jms.connection.CachingConnectionFactory;
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
 * @author Eddú Meléndez
 */
class JmsAutoConfigurationTests {

	private static final String ACTIVEMQ_EMBEDDED_URL = "vm://localhost?broker.persistent=false";

	private static final String ACTIVEMQ_NETWORK_URL = "tcp://localhost:61616";

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ActiveMQAutoConfiguration.class, JmsAutoConfiguration.class));

	@Test
	void testDefaultJmsConfiguration() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class).run(this::testDefaultJmsConfiguration);
	}

	private void testDefaultJmsConfiguration(AssertableApplicationContext loaded) {
		assertThat(loaded).hasSingleBean(ConnectionFactory.class);
		assertThat(loaded).hasSingleBean(CachingConnectionFactory.class);
		CachingConnectionFactory factory = loaded.getBean(CachingConnectionFactory.class);
		assertThat(factory.getTargetConnectionFactory()).isInstanceOf(ActiveMQConnectionFactory.class);
		JmsTemplate jmsTemplate = loaded.getBean(JmsTemplate.class);
		JmsMessagingTemplate messagingTemplate = loaded.getBean(JmsMessagingTemplate.class);
		assertThat(factory).isEqualTo(jmsTemplate.getConnectionFactory());
		assertThat(messagingTemplate.getJmsTemplate()).isEqualTo(jmsTemplate);
		assertThat(getBrokerUrl(factory)).isEqualTo(ACTIVEMQ_EMBEDDED_URL);
		assertThat(loaded.containsBean("jmsListenerContainerFactory")).isTrue();
	}

	@Test
	void testConnectionFactoryBackOff() {
		this.contextRunner.withUserConfiguration(TestConfiguration2.class)
				.run((context) -> assertThat(context.getBean(ActiveMQConnectionFactory.class).getBrokerURL())
						.isEqualTo("foobar"));
	}

	@Test
	void testJmsTemplateBackOff() {
		this.contextRunner.withUserConfiguration(TestConfiguration3.class)
				.run((context) -> assertThat(context.getBean(JmsTemplate.class).getPriority()).isEqualTo(999));
	}

	@Test
	void testJmsMessagingTemplateBackOff() {
		this.contextRunner.withUserConfiguration(TestConfiguration5.class)
				.run((context) -> assertThat(context.getBean(JmsMessagingTemplate.class).getDefaultDestinationName())
						.isEqualTo("fooBar"));
	}

	@Test
	void testJmsTemplateBackOffEverything() {
		this.contextRunner
				.withUserConfiguration(TestConfiguration2.class, TestConfiguration3.class, TestConfiguration5.class)
				.run(this::testJmsTemplateBackOffEverything);
	}

	private void testJmsTemplateBackOffEverything(AssertableApplicationContext loaded) {
		JmsTemplate jmsTemplate = loaded.getBean(JmsTemplate.class);
		assertThat(jmsTemplate.getPriority()).isEqualTo(999);
		assertThat(loaded.getBean(ActiveMQConnectionFactory.class).getBrokerURL()).isEqualTo("foobar");
		JmsMessagingTemplate messagingTemplate = loaded.getBean(JmsMessagingTemplate.class);
		assertThat(messagingTemplate.getDefaultDestinationName()).isEqualTo("fooBar");
		assertThat(messagingTemplate.getJmsTemplate()).isEqualTo(jmsTemplate);
	}

	@Test
	void testEnableJmsCreateDefaultContainerFactory() {
		this.contextRunner.withUserConfiguration(EnableJmsConfiguration.class)
				.run((context) -> assertThat(context)
						.getBean("jmsListenerContainerFactory", JmsListenerContainerFactory.class)
						.isExactlyInstanceOf(DefaultJmsListenerContainerFactory.class));
	}

	@Test
	void testJmsListenerContainerFactoryBackOff() {
		this.contextRunner.withUserConfiguration(TestConfiguration6.class, EnableJmsConfiguration.class)
				.run((context) -> assertThat(context)
						.getBean("jmsListenerContainerFactory", JmsListenerContainerFactory.class)
						.isExactlyInstanceOf(SimpleJmsListenerContainerFactory.class));
	}

	@Test
	void jmsListenerContainerFactoryWhenMultipleConnectionFactoryBeansShouldBackOff() {
		this.contextRunner.withUserConfiguration(TestConfiguration10.class)
				.run((context) -> assertThat(context).doesNotHaveBean(JmsListenerContainerFactory.class));
	}

	@Test
	void testJmsListenerContainerFactoryWithCustomSettings() {
		this.contextRunner.withUserConfiguration(EnableJmsConfiguration.class)
				.withPropertyValues("spring.jms.listener.autoStartup=false",
						"spring.jms.listener.acknowledgeMode=client", "spring.jms.listener.concurrency=2",
						"spring.jms.listener.receiveTimeout=2s", "spring.jms.listener.maxConcurrency=10")
				.run(this::testJmsListenerContainerFactoryWithCustomSettings);
	}

	private void testJmsListenerContainerFactoryWithCustomSettings(AssertableApplicationContext loaded) {
		DefaultMessageListenerContainer container = getContainer(loaded, "jmsListenerContainerFactory");
		assertThat(container.isAutoStartup()).isFalse();
		assertThat(container.getSessionAcknowledgeMode()).isEqualTo(Session.CLIENT_ACKNOWLEDGE);
		assertThat(container.getConcurrentConsumers()).isEqualTo(2);
		assertThat(container.getMaxConcurrentConsumers()).isEqualTo(10);
		assertThat(container).hasFieldOrPropertyWithValue("receiveTimeout", 2000L);
	}

	@Test
	void testJmsListenerContainerFactoryWithDefaultSettings() {
		this.contextRunner.withUserConfiguration(EnableJmsConfiguration.class)
				.run(this::testJmsListenerContainerFactoryWithDefaultSettings);
	}

	private void testJmsListenerContainerFactoryWithDefaultSettings(AssertableApplicationContext loaded) {
		DefaultMessageListenerContainer container = getContainer(loaded, "jmsListenerContainerFactory");
		assertThat(container).hasFieldOrPropertyWithValue("receiveTimeout", 1000L);
	}

	@Test
	void testDefaultContainerFactoryWithJtaTransactionManager() {
		this.contextRunner.withUserConfiguration(TestConfiguration7.class, EnableJmsConfiguration.class)
				.run((context) -> {
					DefaultMessageListenerContainer container = getContainer(context, "jmsListenerContainerFactory");
					assertThat(container.isSessionTransacted()).isFalse();
					assertThat(container).hasFieldOrPropertyWithValue("transactionManager",
							context.getBean(JtaTransactionManager.class));
				});
	}

	@Test
	void testDefaultContainerFactoryNonJtaTransactionManager() {
		this.contextRunner.withUserConfiguration(TestConfiguration8.class, EnableJmsConfiguration.class)
				.run((context) -> {
					DefaultMessageListenerContainer container = getContainer(context, "jmsListenerContainerFactory");
					assertThat(container.isSessionTransacted()).isTrue();
					assertThat(container).hasFieldOrPropertyWithValue("transactionManager", null);
				});
	}

	@Test
	void testDefaultContainerFactoryNoTransactionManager() {
		this.contextRunner.withUserConfiguration(EnableJmsConfiguration.class).run((context) -> {
			DefaultMessageListenerContainer container = getContainer(context, "jmsListenerContainerFactory");
			assertThat(container.isSessionTransacted()).isTrue();
			assertThat(container).hasFieldOrPropertyWithValue("transactionManager", null);
		});
	}

	@Test
	void testDefaultContainerFactoryWithMessageConverters() {
		this.contextRunner.withUserConfiguration(MessageConvertersConfiguration.class, EnableJmsConfiguration.class)
				.run((context) -> {
					DefaultMessageListenerContainer container = getContainer(context, "jmsListenerContainerFactory");
					assertThat(container.getMessageConverter()).isSameAs(context.getBean("myMessageConverter"));
				});
	}

	@Test
	void testDefaultContainerFactoryWithExceptionListener() {
		ExceptionListener exceptionListener = mock(ExceptionListener.class);
		this.contextRunner.withUserConfiguration(EnableJmsConfiguration.class)
				.withBean(ExceptionListener.class, () -> exceptionListener).run((context) -> {
					DefaultMessageListenerContainer container = getContainer(context, "jmsListenerContainerFactory");
					assertThat(container.getExceptionListener()).isSameAs(exceptionListener);
				});
	}

	@Test
	void testCustomContainerFactoryWithConfigurer() {
		this.contextRunner.withUserConfiguration(TestConfiguration9.class, EnableJmsConfiguration.class)
				.withPropertyValues("spring.jms.listener.autoStartup=false").run((context) -> {
					DefaultMessageListenerContainer container = getContainer(context, "customListenerContainerFactory");
					assertThat(container.getCacheLevel()).isEqualTo(DefaultMessageListenerContainer.CACHE_CONSUMER);
					assertThat(container.isAutoStartup()).isFalse();
				});
	}

	private DefaultMessageListenerContainer getContainer(AssertableApplicationContext loaded, String name) {
		JmsListenerContainerFactory<?> factory = loaded.getBean(name, JmsListenerContainerFactory.class);
		assertThat(factory).isInstanceOf(DefaultJmsListenerContainerFactory.class);
		return ((DefaultJmsListenerContainerFactory) factory).createListenerContainer(mock(JmsListenerEndpoint.class));
	}

	@Test
	void testJmsTemplateWithMessageConverter() {
		this.contextRunner.withUserConfiguration(MessageConvertersConfiguration.class).run((context) -> {
			JmsTemplate jmsTemplate = context.getBean(JmsTemplate.class);
			assertThat(jmsTemplate.getMessageConverter()).isSameAs(context.getBean("myMessageConverter"));
		});
	}

	@Test
	void testJmsTemplateWithDestinationResolver() {
		this.contextRunner.withUserConfiguration(DestinationResolversConfiguration.class)
				.run((context) -> assertThat(context.getBean(JmsTemplate.class).getDestinationResolver())
						.isSameAs(context.getBean("myDestinationResolver")));
	}

	@Test
	void testJmsTemplateFullCustomization() {
		this.contextRunner.withUserConfiguration(MessageConvertersConfiguration.class)
				.withPropertyValues("spring.jms.template.default-destination=testQueue",
						"spring.jms.template.delivery-delay=500", "spring.jms.template.delivery-mode=non-persistent",
						"spring.jms.template.priority=6", "spring.jms.template.time-to-live=6000",
						"spring.jms.template.receive-timeout=2000")
				.run((context) -> {
					JmsTemplate jmsTemplate = context.getBean(JmsTemplate.class);
					assertThat(jmsTemplate.getMessageConverter()).isSameAs(context.getBean("myMessageConverter"));
					assertThat(jmsTemplate.isPubSubDomain()).isFalse();
					assertThat(jmsTemplate.getDefaultDestinationName()).isEqualTo("testQueue");
					assertThat(jmsTemplate.getDeliveryDelay()).isEqualTo(500);
					assertThat(jmsTemplate.getDeliveryMode()).isEqualTo(1);
					assertThat(jmsTemplate.getPriority()).isEqualTo(6);
					assertThat(jmsTemplate.getTimeToLive()).isEqualTo(6000);
					assertThat(jmsTemplate.isExplicitQosEnabled()).isTrue();
					assertThat(jmsTemplate.getReceiveTimeout()).isEqualTo(2000);
				});
	}

	@Test
	void testJmsMessagingTemplateUseConfiguredDefaultDestination() {
		this.contextRunner.withPropertyValues("spring.jms.template.default-destination=testQueue").run((context) -> {
			JmsMessagingTemplate messagingTemplate = context.getBean(JmsMessagingTemplate.class);
			assertThat(messagingTemplate.getDefaultDestinationName()).isEqualTo("testQueue");
		});
	}

	@Test
	void testPubSubDisabledByDefault() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.run((context) -> assertThat(context.getBean(JmsTemplate.class).isPubSubDomain()).isFalse());
	}

	@Test
	void testJmsTemplatePostProcessedSoThatPubSubIsTrue() {
		this.contextRunner.withUserConfiguration(TestConfiguration4.class)
				.run((context) -> assertThat(context.getBean(JmsTemplate.class).isPubSubDomain()).isTrue());
	}

	@Test
	void testPubSubDomainActive() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.withPropertyValues("spring.jms.pubSubDomain:true").run((context) -> {
					JmsTemplate jmsTemplate = context.getBean(JmsTemplate.class);
					DefaultMessageListenerContainer defaultMessageListenerContainer = context
							.getBean(DefaultJmsListenerContainerFactory.class)
							.createListenerContainer(mock(JmsListenerEndpoint.class));
					assertThat(jmsTemplate.isPubSubDomain()).isTrue();
					assertThat(defaultMessageListenerContainer.isPubSubDomain()).isTrue();
				});
	}

	@Test
	void testPubSubDomainOverride() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.withPropertyValues("spring.jms.pubSubDomain:false").run((context) -> {
					assertThat(context).hasSingleBean(JmsTemplate.class);
					assertThat(context).hasSingleBean(ConnectionFactory.class);
					JmsTemplate jmsTemplate = context.getBean(JmsTemplate.class);
					ConnectionFactory factory = context.getBean(ConnectionFactory.class);
					assertThat(jmsTemplate).isNotNull();
					assertThat(jmsTemplate.isPubSubDomain()).isFalse();
					assertThat(factory).isNotNull().isEqualTo(jmsTemplate.getConnectionFactory());
				});
	}

	@Test
	void testActiveMQOverriddenStandalone() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.withPropertyValues("spring.activemq.inMemory:false").run((context) -> {
					assertThat(context).hasSingleBean(JmsTemplate.class);
					assertThat(context).hasSingleBean(CachingConnectionFactory.class);
					JmsTemplate jmsTemplate = context.getBean(JmsTemplate.class);
					ConnectionFactory factory = context.getBean(ConnectionFactory.class);
					assertThat(factory).isEqualTo(jmsTemplate.getConnectionFactory());
					assertThat(getBrokerUrl((CachingConnectionFactory) factory)).isEqualTo(ACTIVEMQ_NETWORK_URL);
				});
	}

	@Test
	void testActiveMQOverriddenRemoteHost() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.withPropertyValues("spring.activemq.brokerUrl:tcp://remote-host:10000").run((context) -> {
					assertThat(context).hasSingleBean(JmsTemplate.class);
					assertThat(context).hasSingleBean(CachingConnectionFactory.class);
					JmsTemplate jmsTemplate = context.getBean(JmsTemplate.class);
					ConnectionFactory factory = context.getBean(ConnectionFactory.class);
					assertThat(factory).isEqualTo(jmsTemplate.getConnectionFactory());
					assertThat(getBrokerUrl((CachingConnectionFactory) factory)).isEqualTo("tcp://remote-host:10000");
				});
	}

	private String getBrokerUrl(CachingConnectionFactory connectionFactory) {
		assertThat(connectionFactory.getTargetConnectionFactory()).isInstanceOf(ActiveMQConnectionFactory.class);
		return ((ActiveMQConnectionFactory) connectionFactory.getTargetConnectionFactory()).getBrokerURL();
	}

	@Test
	void testActiveMQOverriddenPool() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.withPropertyValues("spring.activemq.pool.enabled:true").run((context) -> {
					JmsTemplate jmsTemplate = context.getBean(JmsTemplate.class);
					JmsPoolConnectionFactory pool = context.getBean(JmsPoolConnectionFactory.class);
					assertThat(jmsTemplate).isNotNull();
					assertThat(pool).isNotNull();
					assertThat(pool).isEqualTo(jmsTemplate.getConnectionFactory());
					ActiveMQConnectionFactory factory = (ActiveMQConnectionFactory) pool.getConnectionFactory();
					assertThat(factory.getBrokerURL()).isEqualTo(ACTIVEMQ_EMBEDDED_URL);
				});
	}

	@Test
	void testActiveMQOverriddenPoolAndStandalone() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.withPropertyValues("spring.activemq.pool.enabled:true", "spring.activemq.inMemory:false")
				.run((context) -> {
					JmsTemplate jmsTemplate = context.getBean(JmsTemplate.class);
					JmsPoolConnectionFactory pool = context.getBean(JmsPoolConnectionFactory.class);
					assertThat(jmsTemplate).isNotNull();
					assertThat(pool).isNotNull();
					assertThat(pool).isEqualTo(jmsTemplate.getConnectionFactory());
					ActiveMQConnectionFactory factory = (ActiveMQConnectionFactory) pool.getConnectionFactory();
					assertThat(factory.getBrokerURL()).isEqualTo(ACTIVEMQ_NETWORK_URL);
				});
	}

	@Test
	void testActiveMQOverriddenPoolAndRemoteServer() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.withPropertyValues("spring.activemq.pool.enabled:true",
						"spring.activemq.brokerUrl:tcp://remote-host:10000")
				.run((context) -> {
					JmsTemplate jmsTemplate = context.getBean(JmsTemplate.class);
					JmsPoolConnectionFactory pool = context.getBean(JmsPoolConnectionFactory.class);
					assertThat(jmsTemplate).isNotNull();
					assertThat(pool).isNotNull();
					assertThat(pool).isEqualTo(jmsTemplate.getConnectionFactory());
					ActiveMQConnectionFactory factory = (ActiveMQConnectionFactory) pool.getConnectionFactory();
					assertThat(factory.getBrokerURL()).isEqualTo("tcp://remote-host:10000");
				});
	}

	@Test
	void enableJmsAutomatically() {
		this.contextRunner.withUserConfiguration(NoEnableJmsConfiguration.class)
				.run((context) -> assertThat(context)
						.hasBean(JmsListenerConfigUtils.JMS_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME)
						.hasBean(JmsListenerConfigUtils.JMS_LISTENER_ENDPOINT_REGISTRY_BEAN_NAME));
	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration2 {

		@Bean
		ConnectionFactory connectionFactory() {
			return new ActiveMQConnectionFactory() {
				{
					setBrokerURL("foobar");
				}
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration3 {

		@Bean
		JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
			JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
			jmsTemplate.setPriority(999);
			return jmsTemplate;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration4 implements BeanPostProcessor {

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) {
			if (bean.getClass().isAssignableFrom(JmsTemplate.class)) {
				JmsTemplate jmsTemplate = (JmsTemplate) bean;
				jmsTemplate.setPubSubDomain(true);
			}
			return bean;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			return bean;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration5 {

		@Bean
		JmsMessagingTemplate jmsMessagingTemplate(JmsTemplate jmsTemplate) {
			JmsMessagingTemplate messagingTemplate = new JmsMessagingTemplate(jmsTemplate);
			messagingTemplate.setDefaultDestinationName("fooBar");
			return messagingTemplate;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration6 {

		@Bean
		JmsListenerContainerFactory<?> jmsListenerContainerFactory(ConnectionFactory connectionFactory) {
			SimpleJmsListenerContainerFactory factory = new SimpleJmsListenerContainerFactory();
			factory.setConnectionFactory(connectionFactory);
			return factory;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration7 {

		@Bean
		JtaTransactionManager transactionManager() {
			return mock(JtaTransactionManager.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration8 {

		@Bean
		DataSourceTransactionManager transactionManager() {
			return mock(DataSourceTransactionManager.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MessageConvertersConfiguration {

		@Bean
		@Primary
		MessageConverter myMessageConverter() {
			return mock(MessageConverter.class);
		}

		@Bean
		MessageConverter anotherMessageConverter() {
			return mock(MessageConverter.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class DestinationResolversConfiguration {

		@Bean
		@Primary
		DestinationResolver myDestinationResolver() {
			return mock(DestinationResolver.class);
		}

		@Bean
		DestinationResolver anotherDestinationResolver() {
			return mock(DestinationResolver.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration9 {

		@Bean
		JmsListenerContainerFactory<?> customListenerContainerFactory(
				DefaultJmsListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory) {
			DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
			configurer.configure(factory, connectionFactory);
			factory.setCacheLevel(DefaultMessageListenerContainer.CACHE_CONSUMER);
			return factory;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration10 {

		@Bean
		ConnectionFactory connectionFactory1() {
			return new ActiveMQConnectionFactory();
		}

		@Bean
		ConnectionFactory connectionFactory2() {
			return new ActiveMQConnectionFactory();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableJms
	static class EnableJmsConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class NoEnableJmsConfiguration {

	}

}
