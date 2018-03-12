/*
 * Copyright 2012-2018 the original author or authors.
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

import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLSocketFactory;

import com.rabbitmq.client.Address;
import com.rabbitmq.client.Connection;
import org.aopalliance.aop.Advice;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.DirectRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.RabbitListenerConfigUtils;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory.CacheMode;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionNameStrategy;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.support.ValueExpression;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.interceptor.MethodInvocationRecoverer;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link RabbitAutoConfiguration}.
 *
 * @author Greg Turnquist
 * @author Stephane Nicoll
 * @author Gary Russell
 */
public class RabbitAutoConfigurationTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(RabbitAutoConfiguration.class));

	@Test
	public void testDefaultRabbitConfiguration() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.run((context) -> {
					RabbitTemplate rabbitTemplate = context.getBean(RabbitTemplate.class);
					RabbitMessagingTemplate messagingTemplate = context
							.getBean(RabbitMessagingTemplate.class);
					CachingConnectionFactory connectionFactory = context
							.getBean(CachingConnectionFactory.class);
					DirectFieldAccessor dfa = new DirectFieldAccessor(connectionFactory);
					RabbitAdmin amqpAdmin = context.getBean(RabbitAdmin.class);
					assertThat(rabbitTemplate.getConnectionFactory())
							.isEqualTo(connectionFactory);
					assertThat(getMandatory(rabbitTemplate)).isFalse();
					assertThat(messagingTemplate.getRabbitTemplate())
							.isEqualTo(rabbitTemplate);
					assertThat(amqpAdmin).isNotNull();
					assertThat(connectionFactory.getHost()).isEqualTo("localhost");
					assertThat(dfa.getPropertyValue("publisherConfirms"))
							.isEqualTo(false);
					assertThat(dfa.getPropertyValue("publisherReturns")).isEqualTo(false);
					assertThat(context.containsBean("rabbitListenerContainerFactory"))
							.as("Listener container factory should be created by default")
							.isTrue();
				});
	}

	@Test
	public void testDefaultRabbitTemplateConfiguration() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.run((context) -> {
					RabbitTemplate rabbitTemplate = context.getBean(RabbitTemplate.class);
					RabbitTemplate defaultRabbitTemplate = new RabbitTemplate();
					assertThat(rabbitTemplate.getRoutingKey())
							.isEqualTo(defaultRabbitTemplate.getRoutingKey());
					assertThat(rabbitTemplate.getExchange())
							.isEqualTo(defaultRabbitTemplate.getExchange());
				});
	}

	@Test
	public void testDefaultConnectionFactoryConfiguration() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.run((context) -> {
					RabbitProperties properties = new RabbitProperties();
					com.rabbitmq.client.ConnectionFactory rabbitConnectionFactory = getTargetConnectionFactory(
							context);
					assertThat(rabbitConnectionFactory.getUsername())
							.isEqualTo(properties.getUsername());
					assertThat(rabbitConnectionFactory.getPassword())
							.isEqualTo(properties.getPassword());
				});
	}

	@Test
	public void testConnectionFactoryWithOverrides() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.withPropertyValues("spring.rabbitmq.host:remote-server",
						"spring.rabbitmq.port:9000", "spring.rabbitmq.username:alice",
						"spring.rabbitmq.password:secret",
						"spring.rabbitmq.virtual_host:/vhost",
						"spring.rabbitmq.connection-timeout:123")
				.run((context) -> {
					CachingConnectionFactory connectionFactory = context
							.getBean(CachingConnectionFactory.class);
					assertThat(connectionFactory.getHost()).isEqualTo("remote-server");
					assertThat(connectionFactory.getPort()).isEqualTo(9000);
					assertThat(connectionFactory.getVirtualHost()).isEqualTo("/vhost");
					DirectFieldAccessor dfa = new DirectFieldAccessor(connectionFactory);
					com.rabbitmq.client.ConnectionFactory rcf = (com.rabbitmq.client.ConnectionFactory) dfa
							.getPropertyValue("rabbitConnectionFactory");
					assertThat(rcf.getConnectionTimeout()).isEqualTo(123);
					assertThat((Address[]) dfa.getPropertyValue("addresses")).hasSize(1);
				});
	}

	@Test
	public void testConnectionFactoryWithCustomConnectionNameStrategy() {
		this.contextRunner
				.withUserConfiguration(ConnectionNameStrategyConfiguration.class)
				.run((context) -> {
					CachingConnectionFactory connectionFactory = context
							.getBean(CachingConnectionFactory.class);
					DirectFieldAccessor dfa = new DirectFieldAccessor(connectionFactory);
					Address[] addresses = (Address[]) dfa.getPropertyValue("addresses");
					assertThat(addresses).hasSize(1);
					com.rabbitmq.client.ConnectionFactory rcf = mock(
							com.rabbitmq.client.ConnectionFactory.class);
					given(rcf.newConnection(isNull(), eq(addresses), anyString()))
							.willReturn(mock(Connection.class));
					dfa.setPropertyValue("rabbitConnectionFactory", rcf);
					connectionFactory.createConnection();
					verify(rcf).newConnection(isNull(), eq(addresses), eq("test#0"));
					connectionFactory.resetConnection();
					connectionFactory.createConnection();
					verify(rcf).newConnection(isNull(), eq(addresses), eq("test#1"));
				});
	}

	@Test
	public void testConnectionFactoryEmptyVirtualHost() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.withPropertyValues("spring.rabbitmq.virtual_host:").run((context) -> {
					CachingConnectionFactory connectionFactory = context
							.getBean(CachingConnectionFactory.class);
					assertThat(connectionFactory.getVirtualHost()).isEqualTo("/");
				});
	}

	@Test
	public void testConnectionFactoryVirtualHostNoLeadingSlash() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.withPropertyValues("spring.rabbitmq.virtual_host:foo").run((context) -> {
					CachingConnectionFactory connectionFactory = context
							.getBean(CachingConnectionFactory.class);
					assertThat(connectionFactory.getVirtualHost()).isEqualTo("foo");
				});
	}

	@Test
	public void testConnectionFactoryVirtualHostMultiLeadingSlashes() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.withPropertyValues("spring.rabbitmq.virtual_host:///foo")
				.run((context) -> {
					CachingConnectionFactory connectionFactory = context
							.getBean(CachingConnectionFactory.class);
					assertThat(connectionFactory.getVirtualHost()).isEqualTo("///foo");
				});
	}

	@Test
	public void testConnectionFactoryDefaultVirtualHost() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.withPropertyValues("spring.rabbitmq.virtual_host:/").run((context) -> {
					CachingConnectionFactory connectionFactory = context
							.getBean(CachingConnectionFactory.class);
					assertThat(connectionFactory.getVirtualHost()).isEqualTo("/");
				});
	}

	@Test
	public void testConnectionFactoryPublisherSettings() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.withPropertyValues("spring.rabbitmq.publisher-confirms=true",
						"spring.rabbitmq.publisher-returns=true")
				.run((context) -> {
					CachingConnectionFactory connectionFactory = context
							.getBean(CachingConnectionFactory.class);
					RabbitTemplate rabbitTemplate = context.getBean(RabbitTemplate.class);
					DirectFieldAccessor dfa = new DirectFieldAccessor(connectionFactory);
					assertThat(dfa.getPropertyValue("publisherConfirms")).isEqualTo(true);
					assertThat(dfa.getPropertyValue("publisherReturns")).isEqualTo(true);
					assertThat(getMandatory(rabbitTemplate)).isTrue();
				});
	}

	@Test
	public void testRabbitTemplateMessageConverters() {
		this.contextRunner.withUserConfiguration(MessageConvertersConfiguration.class)
				.run((context) -> {
					RabbitTemplate rabbitTemplate = context.getBean(RabbitTemplate.class);
					assertThat(rabbitTemplate.getMessageConverter())
							.isSameAs(context.getBean("myMessageConverter"));
					DirectFieldAccessor dfa = new DirectFieldAccessor(rabbitTemplate);
					assertThat(dfa.getPropertyValue("retryTemplate")).isNull();
				});
	}

	@Test
	public void testRabbitTemplateRetry() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.withPropertyValues("spring.rabbitmq.template.retry.enabled:true",
						"spring.rabbitmq.template.retry.maxAttempts:4",
						"spring.rabbitmq.template.retry.initialInterval:2000",
						"spring.rabbitmq.template.retry.multiplier:1.5",
						"spring.rabbitmq.template.retry.maxInterval:5000",
						"spring.rabbitmq.template.receiveTimeout:123",
						"spring.rabbitmq.template.replyTimeout:456")
				.run((context) -> {
					RabbitTemplate rabbitTemplate = context.getBean(RabbitTemplate.class);
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
				});
	}

	@Test
	public void testRabbitTemplateExchangeAndRoutingKey() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.withPropertyValues("spring.rabbitmq.template.exchange:my-exchange",
						"spring.rabbitmq.template.routing-key:my-routing-key")
				.run((context) -> {
					RabbitTemplate rabbitTemplate = context.getBean(RabbitTemplate.class);
					assertThat(rabbitTemplate.getExchange()).isEqualTo("my-exchange");
					assertThat(rabbitTemplate.getRoutingKey())
							.isEqualTo("my-routing-key");
				});
	}

	@Test
	public void testRabbitTemplateMandatory() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.withPropertyValues("spring.rabbitmq.template.mandatory:true")
				.run((context) -> {
					RabbitTemplate rabbitTemplate = context.getBean(RabbitTemplate.class);
					assertThat(getMandatory(rabbitTemplate)).isTrue();
				});
	}

	@Test
	public void testRabbitTemplateMandatoryDisabledEvenIfPublisherReturnsIsSet() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.withPropertyValues("spring.rabbitmq.template.mandatory:false",
						"spring.rabbitmq.publisher-returns=true")
				.run((context) -> {
					RabbitTemplate rabbitTemplate = context.getBean(RabbitTemplate.class);
					assertThat(getMandatory(rabbitTemplate)).isFalse();
				});
	}

	@Test
	public void testConnectionFactoryBackOff() {
		this.contextRunner.withUserConfiguration(TestConfiguration2.class)
				.run((context) -> {
					RabbitTemplate rabbitTemplate = context.getBean(RabbitTemplate.class);
					CachingConnectionFactory connectionFactory = context
							.getBean(CachingConnectionFactory.class);
					assertThat(connectionFactory)
							.isEqualTo(rabbitTemplate.getConnectionFactory());
					assertThat(connectionFactory.getHost()).isEqualTo("otherserver");
					assertThat(connectionFactory.getPort()).isEqualTo(8001);
				});
	}

	@Test
	public void testConnectionFactoryCacheSettings() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.withPropertyValues("spring.rabbitmq.cache.channel.size=23",
						"spring.rabbitmq.cache.channel.checkoutTimeout=1000",
						"spring.rabbitmq.cache.connection.mode=CONNECTION",
						"spring.rabbitmq.cache.connection.size=2")
				.run((context) -> {
					CachingConnectionFactory connectionFactory = context
							.getBean(CachingConnectionFactory.class);
					DirectFieldAccessor dfa = new DirectFieldAccessor(connectionFactory);
					assertThat(dfa.getPropertyValue("channelCacheSize")).isEqualTo(23);
					assertThat(dfa.getPropertyValue("cacheMode"))
							.isEqualTo(CacheMode.CONNECTION);
					assertThat(dfa.getPropertyValue("connectionCacheSize")).isEqualTo(2);
					assertThat(dfa.getPropertyValue("channelCheckoutTimeout"))
							.isEqualTo(1000L);
				});
	}

	@Test
	public void testRabbitTemplateBackOff() {
		this.contextRunner.withUserConfiguration(TestConfiguration3.class)
				.run((context) -> {
					RabbitTemplate rabbitTemplate = context.getBean(RabbitTemplate.class);
					assertThat(rabbitTemplate.getMessageConverter())
							.isEqualTo(context.getBean("testMessageConverter"));
				});
	}

	@Test
	public void testRabbitMessagingTemplateBackOff() {
		this.contextRunner.withUserConfiguration(TestConfiguration4.class)
				.run((context) -> {
					RabbitMessagingTemplate messagingTemplate = context
							.getBean(RabbitMessagingTemplate.class);
					assertThat(messagingTemplate.getDefaultDestination())
							.isEqualTo("fooBar");
				});
	}

	@Test
	public void testStaticQueues() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.withPropertyValues("spring.rabbitmq.dynamic:false").run((context) -> {
					// There should NOT be an AmqpAdmin bean when dynamic is switch to
					// false
					this.thrown.expect(NoSuchBeanDefinitionException.class);
					this.thrown.expectMessage("No qualifying bean of type");
					this.thrown.expectMessage(AmqpAdmin.class.getName());
					context.getBean(AmqpAdmin.class);
				});
	}

	@Test
	public void testEnableRabbitCreateDefaultContainerFactory() {
		this.contextRunner.withUserConfiguration(EnableRabbitConfiguration.class)
				.run((context) -> {
					RabbitListenerContainerFactory<?> rabbitListenerContainerFactory = context
							.getBean("rabbitListenerContainerFactory",
									RabbitListenerContainerFactory.class);
					assertThat(rabbitListenerContainerFactory.getClass())
							.isEqualTo(SimpleRabbitListenerContainerFactory.class);
				});
	}

	@Test
	public void testRabbitListenerContainerFactoryBackOff() {
		this.contextRunner.withUserConfiguration(TestConfiguration5.class)
				.run((context) -> {
					SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory = context
							.getBean("rabbitListenerContainerFactory",
									SimpleRabbitListenerContainerFactory.class);
					rabbitListenerContainerFactory.setTxSize(10);
					verify(rabbitListenerContainerFactory).setTxSize(10);
					DirectFieldAccessor dfa = new DirectFieldAccessor(
							rabbitListenerContainerFactory);
					Advice[] adviceChain = (Advice[]) dfa.getPropertyValue("adviceChain");
					assertThat(adviceChain).isNull();
				});
	}

	@Test
	public void testSimpleRabbitListenerContainerFactoryWithCustomSettings() {
		this.contextRunner
				.withUserConfiguration(MessageConvertersConfiguration.class,
						MessageRecoverersConfiguration.class)
				.withPropertyValues("spring.rabbitmq.listener.simple.retry.enabled:true",
						"spring.rabbitmq.listener.simple.retry.maxAttempts:4",
						"spring.rabbitmq.listener.simple.retry.initialInterval:2000",
						"spring.rabbitmq.listener.simple.retry.multiplier:1.5",
						"spring.rabbitmq.listener.simple.retry.maxInterval:5000",
						"spring.rabbitmq.listener.simple.autoStartup:false",
						"spring.rabbitmq.listener.simple.acknowledgeMode:manual",
						"spring.rabbitmq.listener.simple.concurrency:5",
						"spring.rabbitmq.listener.simple.maxConcurrency:10",
						"spring.rabbitmq.listener.simple.prefetch:40",
						"spring.rabbitmq.listener.simple.defaultRequeueRejected:false",
						"spring.rabbitmq.listener.simple.idleEventInterval:5",
						"spring.rabbitmq.listener.simple.transactionSize:20")
				.run((context) -> {
					SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory = context
							.getBean("rabbitListenerContainerFactory",
									SimpleRabbitListenerContainerFactory.class);
					DirectFieldAccessor dfa = new DirectFieldAccessor(
							rabbitListenerContainerFactory);
					assertThat(dfa.getPropertyValue("concurrentConsumers")).isEqualTo(5);
					assertThat(dfa.getPropertyValue("maxConcurrentConsumers"))
							.isEqualTo(10);
					assertThat(dfa.getPropertyValue("txSize")).isEqualTo(20);
					checkCommonProps(context, dfa);
				});
	}

	@Test
	public void testDirectRabbitListenerContainerFactoryWithCustomSettings() {
		this.contextRunner
				.withUserConfiguration(MessageConvertersConfiguration.class,
						MessageRecoverersConfiguration.class)
				.withPropertyValues("spring.rabbitmq.listener.type:direct",
						"spring.rabbitmq.listener.direct.retry.enabled:true",
						"spring.rabbitmq.listener.direct.retry.maxAttempts:4",
						"spring.rabbitmq.listener.direct.retry.initialInterval:2000",
						"spring.rabbitmq.listener.direct.retry.multiplier:1.5",
						"spring.rabbitmq.listener.direct.retry.maxInterval:5000",
						"spring.rabbitmq.listener.direct.autoStartup:false",
						"spring.rabbitmq.listener.direct.acknowledgeMode:manual",
						"spring.rabbitmq.listener.direct.consumers-per-queue:5",
						"spring.rabbitmq.listener.direct.prefetch:40",
						"spring.rabbitmq.listener.direct.defaultRequeueRejected:false",
						"spring.rabbitmq.listener.direct.idleEventInterval:5")
				.run((context) -> {
					DirectRabbitListenerContainerFactory rabbitListenerContainerFactory = context
							.getBean("rabbitListenerContainerFactory",
									DirectRabbitListenerContainerFactory.class);
					DirectFieldAccessor dfa = new DirectFieldAccessor(
							rabbitListenerContainerFactory);
					assertThat(dfa.getPropertyValue("consumersPerQueue")).isEqualTo(5);
					checkCommonProps(context, dfa);
				});
	}

	@Test
	public void testRabbitListenerContainerFactoryConfigurersAreAvailable() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.withPropertyValues("spring.rabbitmq.listener.simple.concurrency:5",
						"spring.rabbitmq.listener.simple.maxConcurrency:10",
						"spring.rabbitmq.listener.simple.prefetch:40",
						"spring.rabbitmq.listener.direct.consumers-per-queue:5",
						"spring.rabbitmq.listener.direct.prefetch:40")
				.run((context) -> {
					assertThat(context).hasSingleBean(
							SimpleRabbitListenerContainerFactoryConfigurer.class);
					assertThat(context).hasSingleBean(
							DirectRabbitListenerContainerFactoryConfigurer.class);
				});
	}

	@Test
	public void testSimpleRabbitListenerContainerFactoryConfigurerUsesConfig() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.withPropertyValues("spring.rabbitmq.listener.type:direct",
						"spring.rabbitmq.listener.simple.concurrency:5",
						"spring.rabbitmq.listener.simple.maxConcurrency:10",
						"spring.rabbitmq.listener.simple.prefetch:40")
				.run((context) -> {
					SimpleRabbitListenerContainerFactoryConfigurer configurer = context
							.getBean(
									SimpleRabbitListenerContainerFactoryConfigurer.class);
					SimpleRabbitListenerContainerFactory factory = mock(
							SimpleRabbitListenerContainerFactory.class);
					configurer.configure(factory, mock(ConnectionFactory.class));
					verify(factory).setConcurrentConsumers(5);
					verify(factory).setMaxConcurrentConsumers(10);
					verify(factory).setPrefetchCount(40);
				});
	}

	@Test
	public void testDirectRabbitListenerContainerFactoryConfigurerUsesConfig() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.withPropertyValues("spring.rabbitmq.listener.type:simple",
						"spring.rabbitmq.listener.direct.consumers-per-queue:5",
						"spring.rabbitmq.listener.direct.prefetch:40")
				.run((context) -> {
					DirectRabbitListenerContainerFactoryConfigurer configurer = context
							.getBean(
									DirectRabbitListenerContainerFactoryConfigurer.class);
					DirectRabbitListenerContainerFactory factory = mock(
							DirectRabbitListenerContainerFactory.class);
					configurer.configure(factory, mock(ConnectionFactory.class));
					verify(factory).setConsumersPerQueue(5);
					verify(factory).setPrefetchCount(40);
				});
	}

	private void checkCommonProps(AssertableApplicationContext context,
			DirectFieldAccessor dfa) {
		assertThat(dfa.getPropertyValue("autoStartup")).isEqualTo(Boolean.FALSE);
		assertThat(dfa.getPropertyValue("acknowledgeMode"))
				.isEqualTo(AcknowledgeMode.MANUAL);
		assertThat(dfa.getPropertyValue("prefetchCount")).isEqualTo(40);
		assertThat(dfa.getPropertyValue("messageConverter"))
				.isSameAs(context.getBean("myMessageConverter"));
		assertThat(dfa.getPropertyValue("defaultRequeueRejected"))
				.isEqualTo(Boolean.FALSE);
		assertThat(dfa.getPropertyValue("idleEventInterval")).isEqualTo(5L);
		Advice[] adviceChain = (Advice[]) dfa.getPropertyValue("adviceChain");
		assertThat(adviceChain).isNotNull();
		assertThat(adviceChain.length).isEqualTo(1);
		dfa = new DirectFieldAccessor(adviceChain[0]);
		MessageRecoverer messageRecoverer = context.getBean("myMessageRecoverer",
				MessageRecoverer.class);
		MethodInvocationRecoverer<?> mir = (MethodInvocationRecoverer<?>) dfa
				.getPropertyValue("recoverer");
		Message message = mock(Message.class);
		Exception ex = new Exception("test");
		mir.recover(new Object[] { "foo", message }, ex);
		verify(messageRecoverer).recover(message, ex);
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
	public void enableRabbitAutomatically() {
		this.contextRunner.withUserConfiguration(NoEnableRabbitConfiguration.class)
				.run((context) -> {
					assertThat(context).hasBean(
							RabbitListenerConfigUtils.RABBIT_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME);
					assertThat(context).hasBean(
							RabbitListenerConfigUtils.RABBIT_LISTENER_ENDPOINT_REGISTRY_BEAN_NAME);
				});
	}

	@Test
	public void customizeRequestedHeartBeat() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.withPropertyValues("spring.rabbitmq.requestedHeartbeat:20")
				.run((context) -> {
					com.rabbitmq.client.ConnectionFactory rabbitConnectionFactory = getTargetConnectionFactory(
							context);
					assertThat(rabbitConnectionFactory.getRequestedHeartbeat())
							.isEqualTo(20);
				});
	}

	@Test
	public void noSslByDefault() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.run((context) -> {
					com.rabbitmq.client.ConnectionFactory rabbitConnectionFactory = getTargetConnectionFactory(
							context);
					assertThat(rabbitConnectionFactory.getSocketFactory()).isNull();
					assertThat(rabbitConnectionFactory.isSSL()).isFalse();
				});
	}

	@Test
	public void enableSsl() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.withPropertyValues("spring.rabbitmq.ssl.enabled:true").run((context) -> {
					com.rabbitmq.client.ConnectionFactory rabbitConnectionFactory = getTargetConnectionFactory(
							context);
					assertThat(rabbitConnectionFactory.isSSL()).isTrue();
					assertThat(rabbitConnectionFactory.getSocketFactory())
							.as("SocketFactory must use SSL")
							.isInstanceOf(SSLSocketFactory.class);
				});
	}

	@Test
	// Make sure that we at least attempt to load the store
	public void enableSslWithNonExistingKeystoreShouldFail() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.withPropertyValues("spring.rabbitmq.ssl.enabled:true",
						"spring.rabbitmq.ssl.keyStore=foo",
						"spring.rabbitmq.ssl.keyStorePassword=secret")
				.run((context) -> {
					assertThat(context).hasFailed();
					assertThat(context).getFailure().hasMessageContaining("foo");
					assertThat(context).getFailure()
							.hasMessageContaining("does not exist");
				});
	}

	@Test
	// Make sure that we at least attempt to load the store
	public void enableSslWithNonExistingTrustStoreShouldFail() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.withPropertyValues("spring.rabbitmq.ssl.enabled:true",
						"spring.rabbitmq.ssl.trustStore=bar",
						"spring.rabbitmq.ssl.trustStorePassword=secret")
				.run((context) -> {
					assertThat(context).hasFailed();
					assertThat(context).getFailure().hasMessageContaining("bar");
					assertThat(context).getFailure()
							.hasMessageContaining("does not exist");
				});
	}

	@Test
	public void enableSslWithInvalidKeystoreTypeShouldFail() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.withPropertyValues("spring.rabbitmq.ssl.enabled:true",
						"spring.rabbitmq.ssl.keyStore=foo",
						"spring.rabbitmq.ssl.keyStoreType=fooType")
				.run((context) -> {
					assertThat(context).hasFailed();
					assertThat(context).getFailure().hasMessageContaining("fooType");
					assertThat(context).getFailure()
							.hasRootCauseInstanceOf(NoSuchAlgorithmException.class);
				});
	}

	@Test
	public void enableSslWithInvalidTrustStoreTypeShouldFail() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.withPropertyValues("spring.rabbitmq.ssl.enabled:true",
						"spring.rabbitmq.ssl.trustStore=bar",
						"spring.rabbitmq.ssl.trustStoreType=barType")
				.run((context) -> {
					assertThat(context).hasFailed();
					assertThat(context).getFailure().hasMessageContaining("barType");
					assertThat(context).getFailure()
							.hasRootCauseInstanceOf(NoSuchAlgorithmException.class);
				});
	}

	@Test
	public void enableSslWithKeystoreTypeAndTrustStoreTypeShouldWork() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.withPropertyValues("spring.rabbitmq.ssl.enabled:true",
						"spring.rabbitmq.ssl.keyStore=/org/springframework/boot/autoconfigure/amqp/test.jks",
						"spring.rabbitmq.ssl.keyStoreType=jks",
						"spring.rabbitmq.ssl.keyStorePassword=secret",
						"spring.rabbitmq.ssl.trustStore=/org/springframework/boot/autoconfigure/amqp/test.jks",
						"spring.rabbitmq.ssl.trustStoreType=jks",
						"spring.rabbitmq.ssl.trustStorePassword=secret")
				.run((context) -> assertThat(context).hasNotFailed());
	}

	private com.rabbitmq.client.ConnectionFactory getTargetConnectionFactory(
			AssertableApplicationContext context) {
		CachingConnectionFactory connectionFactory = context
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
	protected static class MessageRecoverersConfiguration {

		@Bean
		@Primary
		public MessageRecoverer myMessageRecoverer() {
			return mock(MessageRecoverer.class);
		}

		@Bean
		public MessageRecoverer anotherMessageRecoverer() {
			return mock(MessageRecoverer.class);
		}

	}

	@Configuration
	protected static class ConnectionNameStrategyConfiguration {

		private final AtomicInteger counter = new AtomicInteger();

		@Bean
		public ConnectionNameStrategy myConnectionNameStrategy() {
			return (connectionFactory) -> "test#" + this.counter.getAndIncrement();
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
