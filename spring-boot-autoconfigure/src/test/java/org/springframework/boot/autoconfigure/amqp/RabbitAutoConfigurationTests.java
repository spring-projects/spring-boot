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

package org.springframework.boot.autoconfigure.amqp;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import com.rabbitmq.client.Address;
import org.aopalliance.aop.Advice;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.RabbitListenerConfigUtils;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory.CacheMode;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.support.ValueExpression;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link RabbitAutoConfiguration}.
 *
 * @author Greg Turnquist
 * @author Stephane Nicoll
 * @author Gary Russell
 * @author Stephane Nicoll
 */
public class RabbitAutoConfigurationTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testDefaultRabbitConfiguration() {
		load(TestConfiguration.class);
		RabbitTemplate rabbitTemplate = this.context.getBean(RabbitTemplate.class);
		RabbitMessagingTemplate messagingTemplate = this.context
				.getBean(RabbitMessagingTemplate.class);
		CachingConnectionFactory connectionFactory = this.context
				.getBean(CachingConnectionFactory.class);
		DirectFieldAccessor dfa = new DirectFieldAccessor(connectionFactory);
		RabbitAdmin amqpAdmin = this.context.getBean(RabbitAdmin.class);
		assertThat(rabbitTemplate.getConnectionFactory()).isEqualTo(connectionFactory);
		assertThat(getMandatory(rabbitTemplate)).isFalse();
		assertThat(messagingTemplate.getRabbitTemplate()).isEqualTo(rabbitTemplate);
		assertThat(amqpAdmin).isNotNull();
		assertThat(connectionFactory.getHost()).isEqualTo("localhost");
		assertThat(dfa.getPropertyValue("publisherConfirms")).isEqualTo(false);
		assertThat(dfa.getPropertyValue("publisherReturns")).isEqualTo(false);
		assertThat(this.context.containsBean("rabbitListenerContainerFactory"))
				.as("Listener container factory should be created by default").isTrue();
	}

	@Test
	public void testConnectionFactoryWithOverrides() {
		load(TestConfiguration.class, "spring.rabbitmq.host:remote-server",
				"spring.rabbitmq.port:9000", "spring.rabbitmq.username:alice",
				"spring.rabbitmq.password:secret", "spring.rabbitmq.virtual_host:/vhost",
				"spring.rabbitmq.connection-timeout:123");
		CachingConnectionFactory connectionFactory = this.context
				.getBean(CachingConnectionFactory.class);
		assertThat(connectionFactory.getHost()).isEqualTo("remote-server");
		assertThat(connectionFactory.getPort()).isEqualTo(9000);
		assertThat(connectionFactory.getVirtualHost()).isEqualTo("/vhost");
		DirectFieldAccessor dfa = new DirectFieldAccessor(connectionFactory);
		com.rabbitmq.client.ConnectionFactory rcf = (com.rabbitmq.client.ConnectionFactory) dfa
				.getPropertyValue("rabbitConnectionFactory");
		assertThat(rcf.getConnectionTimeout()).isEqualTo(123);
		assertThat((Address[]) dfa.getPropertyValue("addresses")).hasSize(1);

	}

	@Test
	public void testConnectionFactoryEmptyVirtualHost() {
		load(TestConfiguration.class, "spring.rabbitmq.virtual_host:");
		CachingConnectionFactory connectionFactory = this.context
				.getBean(CachingConnectionFactory.class);
		assertThat(connectionFactory.getVirtualHost()).isEqualTo("/");
	}

	@Test
	public void testConnectionFactoryVirtualHostNoLeadingSlash() {
		load(TestConfiguration.class, "spring.rabbitmq.virtual_host:foo");
		CachingConnectionFactory connectionFactory = this.context
				.getBean(CachingConnectionFactory.class);
		assertThat(connectionFactory.getVirtualHost()).isEqualTo("foo");
	}

	@Test
	public void testConnectionFactoryVirtualHostMultiLeadingSlashes() {
		load(TestConfiguration.class, "spring.rabbitmq.virtual_host:///foo");
		CachingConnectionFactory connectionFactory = this.context
				.getBean(CachingConnectionFactory.class);
		assertThat(connectionFactory.getVirtualHost()).isEqualTo("///foo");
	}

	@Test
	public void testConnectionFactoryDefaultVirtualHost() {
		load(TestConfiguration.class, "spring.rabbitmq.virtual_host:/");
		CachingConnectionFactory connectionFactory = this.context
				.getBean(CachingConnectionFactory.class);
		assertThat(connectionFactory.getVirtualHost()).isEqualTo("/");
	}

	@Test
	public void testConnectionFactoryPublisherSettings() {
		load(TestConfiguration.class, "spring.rabbitmq.publisher-confirms=true",
				"spring.rabbitmq.publisher-returns=true");
		CachingConnectionFactory connectionFactory = this.context
				.getBean(CachingConnectionFactory.class);
		RabbitTemplate rabbitTemplate = this.context.getBean(RabbitTemplate.class);
		DirectFieldAccessor dfa = new DirectFieldAccessor(connectionFactory);
		assertThat(dfa.getPropertyValue("publisherConfirms")).isEqualTo(true);
		assertThat(dfa.getPropertyValue("publisherReturns")).isEqualTo(true);
		assertThat(getMandatory(rabbitTemplate)).isTrue();
	}

	@Test
	public void testRabbitTemplateMessageConverters() {
		load(MessageConvertersConfiguration.class);
		RabbitTemplate rabbitTemplate = this.context.getBean(RabbitTemplate.class);
		assertThat(rabbitTemplate.getMessageConverter())
				.isSameAs(this.context.getBean("myMessageConverter"));
		DirectFieldAccessor dfa = new DirectFieldAccessor(rabbitTemplate);
		assertThat(dfa.getPropertyValue("retryTemplate")).isNull();
	}

	@Test
	public void testRabbitTemplateRetry() {
		load(TestConfiguration.class, "spring.rabbitmq.template.retry.enabled:true",
				"spring.rabbitmq.template.retry.maxAttempts:4",
				"spring.rabbitmq.template.retry.initialInterval:2000",
				"spring.rabbitmq.template.retry.multiplier:1.5",
				"spring.rabbitmq.template.retry.maxInterval:5000",
				"spring.rabbitmq.template.receiveTimeout:123",
				"spring.rabbitmq.template.replyTimeout:456");
		RabbitTemplate rabbitTemplate = this.context.getBean(RabbitTemplate.class);
		DirectFieldAccessor dfa = new DirectFieldAccessor(rabbitTemplate);
		assertThat(dfa.getPropertyValue("receiveTimeout")).isEqualTo(123L);
		assertThat(dfa.getPropertyValue("replyTimeout")).isEqualTo(456L);
		RetryTemplate retryTemplate = (RetryTemplate) dfa
				.getPropertyValue("retryTemplate");
		assertThat(retryTemplate).isNotNull();
		dfa = new DirectFieldAccessor(retryTemplate);
		SimpleRetryPolicy retryPolicy = (SimpleRetryPolicy) dfa
				.getPropertyValue("retryPolicy");
		ExponentialBackOffPolicy backOffPolicy = (ExponentialBackOffPolicy) dfa
				.getPropertyValue("backOffPolicy");
		assertThat(retryPolicy.getMaxAttempts()).isEqualTo(4);
		assertThat(backOffPolicy.getInitialInterval()).isEqualTo(2000);
		assertThat(backOffPolicy.getMultiplier()).isEqualTo(1.5);
		assertThat(backOffPolicy.getMaxInterval()).isEqualTo(5000);
	}

	@Test
	public void testRabbitTemplateMandatory() {
		load(TestConfiguration.class, "spring.rabbitmq.template.mandatory:true");
		RabbitTemplate rabbitTemplate = this.context.getBean(RabbitTemplate.class);
		assertThat(getMandatory(rabbitTemplate)).isTrue();
	}

	@Test
	public void testRabbitTemplateMandatoryDisabledEvenIfPublisherReturnsIsSet() {
		load(TestConfiguration.class, "spring.rabbitmq.template.mandatory:false",
				"spring.rabbitmq.publisher-returns=true");
		RabbitTemplate rabbitTemplate = this.context.getBean(RabbitTemplate.class);
		assertThat(getMandatory(rabbitTemplate)).isFalse();
	}

	@Test
	public void testConnectionFactoryBackOff() {
		load(TestConfiguration2.class);
		RabbitTemplate rabbitTemplate = this.context.getBean(RabbitTemplate.class);
		CachingConnectionFactory connectionFactory = this.context
				.getBean(CachingConnectionFactory.class);
		assertThat(connectionFactory).isEqualTo(rabbitTemplate.getConnectionFactory());
		assertThat(connectionFactory.getHost()).isEqualTo("otherserver");
		assertThat(connectionFactory.getPort()).isEqualTo(8001);
	}

	@Test
	public void testConnectionFactoryCacheSettings() {
		load(TestConfiguration.class, "spring.rabbitmq.cache.channel.size=23",
				"spring.rabbitmq.cache.channel.checkoutTimeout=1000",
				"spring.rabbitmq.cache.connection.mode=CONNECTION",
				"spring.rabbitmq.cache.connection.size=2");
		CachingConnectionFactory connectionFactory = this.context
				.getBean(CachingConnectionFactory.class);
		DirectFieldAccessor dfa = new DirectFieldAccessor(connectionFactory);
		assertThat(dfa.getPropertyValue("channelCacheSize")).isEqualTo(23);
		assertThat(dfa.getPropertyValue("cacheMode")).isEqualTo(CacheMode.CONNECTION);
		assertThat(dfa.getPropertyValue("connectionCacheSize")).isEqualTo(2);
		assertThat(dfa.getPropertyValue("channelCheckoutTimeout")).isEqualTo(1000L);
	}

	@Test
	public void testRabbitTemplateBackOff() {
		load(TestConfiguration3.class);
		RabbitTemplate rabbitTemplate = this.context.getBean(RabbitTemplate.class);
		assertThat(rabbitTemplate.getMessageConverter())
				.isEqualTo(this.context.getBean("testMessageConverter"));
	}

	@Test
	public void testRabbitMessagingTemplateBackOff() {
		load(TestConfiguration4.class);
		RabbitMessagingTemplate messagingTemplate = this.context
				.getBean(RabbitMessagingTemplate.class);
		assertThat(messagingTemplate.getDefaultDestination()).isEqualTo("fooBar");
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
		assertThat(rabbitListenerContainerFactory.getClass())
				.isEqualTo(SimpleRabbitListenerContainerFactory.class);
	}

	@Test
	public void testRabbitListenerContainerFactoryBackOff() {
		load(TestConfiguration5.class);
		SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory = this.context
				.getBean("rabbitListenerContainerFactory",
						SimpleRabbitListenerContainerFactory.class);
		rabbitListenerContainerFactory.setTxSize(10);
		verify(rabbitListenerContainerFactory).setTxSize(10);
		DirectFieldAccessor dfa = new DirectFieldAccessor(rabbitListenerContainerFactory);
		Advice[] adviceChain = (Advice[]) dfa.getPropertyValue("adviceChain");
		assertThat(adviceChain).isNull();
	}

	@Test
	public void testRabbitListenerContainerFactoryWithCustomSettings() {
		load(MessageConvertersConfiguration.class,
				"spring.rabbitmq.listener.retry.enabled:true",
				"spring.rabbitmq.listener.retry.maxAttempts:4",
				"spring.rabbitmq.listener.retry.initialInterval:2000",
				"spring.rabbitmq.listener.retry.multiplier:1.5",
				"spring.rabbitmq.listener.retry.maxInterval:5000",
				"spring.rabbitmq.listener.autoStartup:false",
				"spring.rabbitmq.listener.acknowledgeMode:manual",
				"spring.rabbitmq.listener.concurrency:5",
				"spring.rabbitmq.listener.maxConcurrency:10",
				"spring.rabbitmq.listener.prefetch:40",
				"spring.rabbitmq.listener.defaultRequeueRejected:false",
				"spring.rabbitmq.listener.transactionSize:20");
		SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory = this.context
				.getBean("rabbitListenerContainerFactory",
						SimpleRabbitListenerContainerFactory.class);
		DirectFieldAccessor dfa = new DirectFieldAccessor(rabbitListenerContainerFactory);
		assertThat(dfa.getPropertyValue("autoStartup")).isEqualTo(Boolean.FALSE);
		assertThat(dfa.getPropertyValue("acknowledgeMode"))
				.isEqualTo(AcknowledgeMode.MANUAL);
		assertThat(dfa.getPropertyValue("concurrentConsumers")).isEqualTo(5);
		assertThat(dfa.getPropertyValue("maxConcurrentConsumers")).isEqualTo(10);
		assertThat(dfa.getPropertyValue("prefetchCount")).isEqualTo(40);
		assertThat(dfa.getPropertyValue("txSize")).isEqualTo(20);
		assertThat(dfa.getPropertyValue("messageConverter"))
				.isSameAs(this.context.getBean("myMessageConverter"));
		assertThat(dfa.getPropertyValue("defaultRequeueRejected"))
				.isEqualTo(Boolean.FALSE);
		Advice[] adviceChain = (Advice[]) dfa.getPropertyValue("adviceChain");
		assertThat(adviceChain).isNotNull();
		assertThat(adviceChain.length).isEqualTo(1);
		dfa = new DirectFieldAccessor(adviceChain[0]);
		RetryTemplate retryTemplate = (RetryTemplate) dfa
				.getPropertyValue("retryOperations");
		assertThat(retryTemplate).isNotNull();
		dfa = new DirectFieldAccessor(retryTemplate);
		SimpleRetryPolicy retryPolicy = (SimpleRetryPolicy) dfa
				.getPropertyValue("retryPolicy");
		ExponentialBackOffPolicy backOffPolicy = (ExponentialBackOffPolicy) dfa
				.getPropertyValue("backOffPolicy");
		assertThat(retryPolicy.getMaxAttempts()).isEqualTo(4);
		assertThat(backOffPolicy.getInitialInterval()).isEqualTo(2000);
		assertThat(backOffPolicy.getMultiplier()).isEqualTo(1.5);
		assertThat(backOffPolicy.getMaxInterval()).isEqualTo(5000);
	}

	@Test
	public void enableRabbitAutomatically() throws Exception {
		load(NoEnableRabbitConfiguration.class);
		AnnotationConfigApplicationContext ctx = this.context;
		ctx.getBean(
				RabbitListenerConfigUtils.RABBIT_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME);
		ctx.getBean(
				RabbitListenerConfigUtils.RABBIT_LISTENER_ENDPOINT_REGISTRY_BEAN_NAME);
	}

	@Test
	public void customizeRequestedHeartBeat() {
		load(TestConfiguration.class, "spring.rabbitmq.requestedHeartbeat:20");
		com.rabbitmq.client.ConnectionFactory rabbitConnectionFactory = getTargetConnectionFactory();
		assertThat(rabbitConnectionFactory.getRequestedHeartbeat()).isEqualTo(20);
	}

	@Test
	public void noSslByDefault() {
		load(TestConfiguration.class);
		com.rabbitmq.client.ConnectionFactory rabbitConnectionFactory = getTargetConnectionFactory();
		assertThat(rabbitConnectionFactory.getSocketFactory())
				.as("Must use default SocketFactory")
				.isEqualTo(SocketFactory.getDefault());
	}

	@Test
	public void enableSsl() {
		load(TestConfiguration.class, "spring.rabbitmq.ssl.enabled:true");
		com.rabbitmq.client.ConnectionFactory rabbitConnectionFactory = getTargetConnectionFactory();
		assertThat(rabbitConnectionFactory.getSocketFactory())
				.as("SocketFactory must use SSL").isInstanceOf(SSLSocketFactory.class);
	}

	@Test
	// Make sure that we at least attempt to load the store
	public void enableSslWithExtraConfig() {
		this.thrown.expectMessage("foo");
		this.thrown.expectMessage("does not exist");
		load(TestConfiguration.class, "spring.rabbitmq.ssl.enabled:true",
				"spring.rabbitmq.ssl.keyStore=foo",
				"spring.rabbitmq.ssl.keyStorePassword=secret",
				"spring.rabbitmq.ssl.trustStore=bar",
				"spring.rabbitmq.ssl.trustStorePassword=secret");
	}

	private com.rabbitmq.client.ConnectionFactory getTargetConnectionFactory() {
		CachingConnectionFactory connectionFactory = this.context
				.getBean(CachingConnectionFactory.class);
		return (com.rabbitmq.client.ConnectionFactory) new DirectFieldAccessor(
				connectionFactory).getPropertyValue("rabbitConnectionFactory");
	}

	@SuppressWarnings("unchecked")
	private boolean getMandatory(RabbitTemplate rabbitTemplate) {
		ValueExpression<Boolean> expression = (ValueExpression<Boolean>) new DirectFieldAccessor(
				rabbitTemplate).getPropertyValue("mandatoryExpression");
		return expression.getValue();
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
	@EnableRabbit
	protected static class EnableRabbitConfiguration {

	}

	@Configuration
	protected static class NoEnableRabbitConfiguration {

	}

}
