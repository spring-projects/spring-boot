/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.amqp;

import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import com.rabbitmq.client.Address;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.SslContextFactory;
import com.rabbitmq.client.TrustEverythingTrustManager;
import org.aopalliance.aop.Advice;
import org.junit.Test;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.AbstractRabbitListenerContainerFactory;
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
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.interceptor.MethodInvocationRecoverer;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
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
					RabbitAdmin amqpAdmin = context.getBean(RabbitAdmin.class);
					assertThat(rabbitTemplate.getConnectionFactory())
							.isEqualTo(connectionFactory);
					assertThat(getMandatory(rabbitTemplate)).isFalse();
					assertThat(messagingTemplate.getRabbitTemplate())
							.isEqualTo(rabbitTemplate);
					assertThat(amqpAdmin).isNotNull();
					assertThat(connectionFactory.getHost()).isEqualTo("localhost");
					assertThat(connectionFactory.isPublisherConfirms()).isFalse();
					assertThat(connectionFactory.isPublisherReturns()).isFalse();
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
					com.rabbitmq.client.ConnectionFactory rcf = connectionFactory
							.getRabbitConnectionFactory();
					assertThat(rcf.getConnectionTimeout()).isEqualTo(123);
					assertThat((Address[]) ReflectionTestUtils.getField(connectionFactory,
							"addresses")).hasSize(1);
				});
	}

	@Test
	public void testConnectionFactoryWithCustomConnectionNameStrategy() {
		this.contextRunner
				.withUserConfiguration(ConnectionNameStrategyConfiguration.class)
				.run((context) -> {
					CachingConnectionFactory connectionFactory = context
							.getBean(CachingConnectionFactory.class);
					Address[] addresses = (Address[]) ReflectionTestUtils
							.getField(connectionFactory, "addresses");
					assertThat(addresses).hasSize(1);
					com.rabbitmq.client.ConnectionFactory rcf = mock(
							com.rabbitmq.client.ConnectionFactory.class);
					given(rcf.newConnection(isNull(), eq(addresses), anyString()))
							.willReturn(mock(Connection.class));
					ReflectionTestUtils.setField(connectionFactory,
							"rabbitConnectionFactory", rcf);
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
					assertThat(connectionFactory.isPublisherConfirms()).isTrue();
					assertThat(connectionFactory.isPublisherReturns()).isTrue();
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
					assertThat(rabbitTemplate)
							.hasFieldOrPropertyWithValue("retryTemplate", null);
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
					assertThat(rabbitTemplate)
							.hasFieldOrPropertyWithValue("receiveTimeout", 123L);
					assertThat(rabbitTemplate).hasFieldOrPropertyWithValue("replyTimeout",
							456L);
					RetryTemplate retryTemplate = (RetryTemplate) ReflectionTestUtils
							.getField(rabbitTemplate, "retryTemplate");
					assertThat(retryTemplate).isNotNull();
					SimpleRetryPolicy retryPolicy = (SimpleRetryPolicy) ReflectionTestUtils
							.getField(retryTemplate, "retryPolicy");
					ExponentialBackOffPolicy backOffPolicy = (ExponentialBackOffPolicy) ReflectionTestUtils
							.getField(retryTemplate, "backOffPolicy");
					assertThat(retryPolicy.getMaxAttempts()).isEqualTo(4);
					assertThat(backOffPolicy.getInitialInterval()).isEqualTo(2000);
					assertThat(backOffPolicy.getMultiplier()).isEqualTo(1.5);
					assertThat(backOffPolicy.getMaxInterval()).isEqualTo(5000);
				});
	}

	@Test
	public void testRabbitTemplateRetryWithCustomizer() {
		this.contextRunner
				.withUserConfiguration(RabbitRetryTemplateCustomizerConfiguration.class)
				.withPropertyValues("spring.rabbitmq.template.retry.enabled:true",
						"spring.rabbitmq.template.retry.initialInterval:2000")
				.run((context) -> {
					RabbitTemplate rabbitTemplate = context.getBean(RabbitTemplate.class);
					RetryTemplate retryTemplate = (RetryTemplate) ReflectionTestUtils
							.getField(rabbitTemplate, "retryTemplate");
					assertThat(retryTemplate).isNotNull();
					ExponentialBackOffPolicy backOffPolicy = (ExponentialBackOffPolicy) ReflectionTestUtils
							.getField(retryTemplate, "backOffPolicy");
					assertThat(backOffPolicy).isSameAs(context.getBean(
							RabbitRetryTemplateCustomizerConfiguration.class).backOffPolicy);
					assertThat(backOffPolicy.getInitialInterval()).isEqualTo(100);
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
	public void testRabbitTemplateDefaultReceiveQueue() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.withPropertyValues(
						"spring.rabbitmq.template.default-receive-queue:default-queue")
				.run((context) -> {
					RabbitTemplate rabbitTemplate = context.getBean(RabbitTemplate.class);
					assertThat(rabbitTemplate).hasFieldOrPropertyWithValue(
							"defaultReceiveQueue", "default-queue");
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
					assertThat(connectionFactory.getChannelCacheSize()).isEqualTo(23);
					assertThat(connectionFactory.getCacheMode())
							.isEqualTo(CacheMode.CONNECTION);
					assertThat(connectionFactory.getConnectionCacheSize()).isEqualTo(2);
					assertThat(connectionFactory)
							.hasFieldOrPropertyWithValue("channelCheckoutTimeout", 1000L);
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
		// There should NOT be an AmqpAdmin bean when dynamic is switch to false
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.withPropertyValues("spring.rabbitmq.dynamic:false")
				.run((context) -> assertThatExceptionOfType(
						NoSuchBeanDefinitionException.class)
								.isThrownBy(() -> context.getBean(AmqpAdmin.class))
								.withMessageContaining("No qualifying bean of type '"
										+ AmqpAdmin.class.getName() + "'"));
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
					assertThat(rabbitListenerContainerFactory.getAdviceChain()).isNull();
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
						"spring.rabbitmq.listener.simple.transactionSize:20",
						"spring.rabbitmq.listener.simple.missingQueuesFatal:false")
				.run((context) -> {
					SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory = context
							.getBean("rabbitListenerContainerFactory",
									SimpleRabbitListenerContainerFactory.class);
					assertThat(rabbitListenerContainerFactory)
							.hasFieldOrPropertyWithValue("concurrentConsumers", 5);
					assertThat(rabbitListenerContainerFactory)
							.hasFieldOrPropertyWithValue("maxConcurrentConsumers", 10);
					assertThat(rabbitListenerContainerFactory)
							.hasFieldOrPropertyWithValue("txSize", 20);
					assertThat(rabbitListenerContainerFactory)
							.hasFieldOrPropertyWithValue("missingQueuesFatal", false);
					checkCommonProps(context, rabbitListenerContainerFactory);
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
						"spring.rabbitmq.listener.direct.idleEventInterval:5",
						"spring.rabbitmq.listener.direct.missingQueuesFatal:true")
				.run((context) -> {
					DirectRabbitListenerContainerFactory rabbitListenerContainerFactory = context
							.getBean("rabbitListenerContainerFactory",
									DirectRabbitListenerContainerFactory.class);
					assertThat(rabbitListenerContainerFactory)
							.hasFieldOrPropertyWithValue("consumersPerQueue", 5);
					assertThat(rabbitListenerContainerFactory)
							.hasFieldOrPropertyWithValue("missingQueuesFatal", true);
					checkCommonProps(context, rabbitListenerContainerFactory);
				});
	}

	@Test
	public void testSimpleRabbitListenerContainerFactoryRetryWithCustomizer() {
		this.contextRunner
				.withUserConfiguration(RabbitRetryTemplateCustomizerConfiguration.class)
				.withPropertyValues("spring.rabbitmq.listener.simple.retry.enabled:true",
						"spring.rabbitmq.listener.simple.retry.maxAttempts:4")
				.run((context) -> {
					SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory = context
							.getBean("rabbitListenerContainerFactory",
									SimpleRabbitListenerContainerFactory.class);
					assertListenerRetryTemplate(rabbitListenerContainerFactory,
							context.getBean(
									RabbitRetryTemplateCustomizerConfiguration.class).retryPolicy);
				});
	}

	@Test
	public void testDirectRabbitListenerContainerFactoryRetryWithCustomizer() {
		this.contextRunner
				.withUserConfiguration(RabbitRetryTemplateCustomizerConfiguration.class)
				.withPropertyValues("spring.rabbitmq.listener.type:direct",
						"spring.rabbitmq.listener.direct.retry.enabled:true",
						"spring.rabbitmq.listener.direct.retry.maxAttempts:4")
				.run((context) -> {
					DirectRabbitListenerContainerFactory rabbitListenerContainerFactory = context
							.getBean("rabbitListenerContainerFactory",
									DirectRabbitListenerContainerFactory.class);
					assertListenerRetryTemplate(rabbitListenerContainerFactory,
							context.getBean(
									RabbitRetryTemplateCustomizerConfiguration.class).retryPolicy);
				});
	}

	private void assertListenerRetryTemplate(
			AbstractRabbitListenerContainerFactory<?> rabbitListenerContainerFactory,
			RetryPolicy retryPolicy) {
		Advice[] adviceChain = rabbitListenerContainerFactory.getAdviceChain();
		assertThat(adviceChain).isNotNull();
		assertThat(adviceChain.length).isEqualTo(1);
		Advice advice = adviceChain[0];
		RetryTemplate retryTemplate = (RetryTemplate) ReflectionTestUtils.getField(advice,
				"retryOperations");
		assertThat(retryTemplate).hasFieldOrPropertyWithValue("retryPolicy", retryPolicy);
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
			AbstractRabbitListenerContainerFactory<?> containerFactory) {
		assertThat(containerFactory).hasFieldOrPropertyWithValue("autoStartup",
				Boolean.FALSE);
		assertThat(containerFactory).hasFieldOrPropertyWithValue("acknowledgeMode",
				AcknowledgeMode.MANUAL);
		assertThat(containerFactory).hasFieldOrPropertyWithValue("prefetchCount", 40);
		assertThat(containerFactory).hasFieldOrPropertyWithValue("messageConverter",
				context.getBean("myMessageConverter"));
		assertThat(containerFactory).hasFieldOrPropertyWithValue("defaultRequeueRejected",
				Boolean.FALSE);
		assertThat(containerFactory).hasFieldOrPropertyWithValue("idleEventInterval", 5L);
		Advice[] adviceChain = containerFactory.getAdviceChain();
		assertThat(adviceChain).isNotNull();
		assertThat(adviceChain.length).isEqualTo(1);
		Advice advice = adviceChain[0];
		MessageRecoverer messageRecoverer = context.getBean("myMessageRecoverer",
				MessageRecoverer.class);
		MethodInvocationRecoverer<?> mir = (MethodInvocationRecoverer<?>) ReflectionTestUtils
				.getField(advice, "recoverer");
		Message message = mock(Message.class);
		Exception ex = new Exception("test");
		mir.recover(new Object[] { "foo", message }, ex);
		verify(messageRecoverer).recover(message, ex);
		RetryTemplate retryTemplate = (RetryTemplate) ReflectionTestUtils.getField(advice,
				"retryOperations");
		assertThat(retryTemplate).isNotNull();
		SimpleRetryPolicy retryPolicy = (SimpleRetryPolicy) ReflectionTestUtils
				.getField(retryTemplate, "retryPolicy");
		ExponentialBackOffPolicy backOffPolicy = (ExponentialBackOffPolicy) ReflectionTestUtils
				.getField(retryTemplate, "backOffPolicy");
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

	@Test
	public void enableSslWithValidateServerCertificateFalse() throws Exception {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.withPropertyValues("spring.rabbitmq.ssl.enabled:true",
						"spring.rabbitmq.ssl.validateServerCertificate=false")
				.run((context) -> {
					com.rabbitmq.client.ConnectionFactory rabbitConnectionFactory = getTargetConnectionFactory(
							context);
					TrustManager trustManager = getTrustManager(rabbitConnectionFactory);
					assertThat(trustManager)
							.isInstanceOf(TrustEverythingTrustManager.class);
				});
	}

	@Test
	public void enableSslWithValidateServerCertificateDefault() throws Exception {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.withPropertyValues("spring.rabbitmq.ssl.enabled:true").run((context) -> {
					com.rabbitmq.client.ConnectionFactory rabbitConnectionFactory = getTargetConnectionFactory(
							context);
					TrustManager trustManager = getTrustManager(rabbitConnectionFactory);
					assertThat(trustManager)
							.isNotInstanceOf(TrustEverythingTrustManager.class);
				});
	}

	private TrustManager getTrustManager(
			com.rabbitmq.client.ConnectionFactory rabbitConnectionFactory) {
		SslContextFactory sslContextFactory = (SslContextFactory) ReflectionTestUtils
				.getField(rabbitConnectionFactory, "sslContextFactory");
		SSLContext sslContext = sslContextFactory.create("connection");
		Object spi = ReflectionTestUtils.getField(sslContext, "contextSpi");
		Object trustManager = ReflectionTestUtils.getField(spi, "trustManager");
		while (trustManager.getClass().getName().endsWith("Wrapper")) {
			trustManager = ReflectionTestUtils.getField(trustManager, "tm");
		}
		return (TrustManager) trustManager;
	}

	private com.rabbitmq.client.ConnectionFactory getTargetConnectionFactory(
			AssertableApplicationContext context) {
		CachingConnectionFactory connectionFactory = context
				.getBean(CachingConnectionFactory.class);
		return connectionFactory.getRabbitConnectionFactory();
	}

	private boolean getMandatory(RabbitTemplate rabbitTemplate) {
		return rabbitTemplate.isMandatoryFor(mock(Message.class));
	}

	@Configuration(proxyBeanMethods = false)
	protected static class TestConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	protected static class TestConfiguration2 {

		@Bean
		ConnectionFactory aDifferentConnectionFactory() {
			return new CachingConnectionFactory("otherserver", 8001);
		}

	}

	@Configuration(proxyBeanMethods = false)
	protected static class TestConfiguration3 {

		@Bean
		RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
				MessageConverter messageConverter) {
			RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
			rabbitTemplate.setMessageConverter(messageConverter);
			return rabbitTemplate;
		}

		@Bean
		public MessageConverter testMessageConverter() {
			return mock(MessageConverter.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	protected static class TestConfiguration4 {

		@Bean
		RabbitMessagingTemplate messagingTemplate(RabbitTemplate rabbitTemplate) {
			RabbitMessagingTemplate messagingTemplate = new RabbitMessagingTemplate(
					rabbitTemplate);
			messagingTemplate.setDefaultDestination("fooBar");
			return messagingTemplate;
		}

	}

	@Configuration(proxyBeanMethods = false)
	protected static class TestConfiguration5 {

		@Bean
		RabbitListenerContainerFactory<?> rabbitListenerContainerFactory() {
			return mock(SimpleRabbitListenerContainerFactory.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
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

	@Configuration(proxyBeanMethods = false)
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

	@Configuration(proxyBeanMethods = false)
	protected static class ConnectionNameStrategyConfiguration {

		private final AtomicInteger counter = new AtomicInteger();

		@Bean
		public ConnectionNameStrategy myConnectionNameStrategy() {
			return (connectionFactory) -> "test#" + this.counter.getAndIncrement();
		}

	}

	@Configuration(proxyBeanMethods = false)
	protected static class RabbitRetryTemplateCustomizerConfiguration {

		private final BackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();

		private final RetryPolicy retryPolicy = new NeverRetryPolicy();

		@Bean
		public RabbitRetryTemplateCustomizer rabbitTemplateRetryTemplateCustomizer() {
			return (target, template) -> {
				if (target.equals(RabbitRetryTemplateCustomizer.Target.SENDER)) {
					template.setBackOffPolicy(this.backOffPolicy);
				}
			};
		}

		@Bean
		public RabbitRetryTemplateCustomizer rabbitListenerRetryTemplateCustomizer() {
			return (target, template) -> {
				if (target.equals(RabbitRetryTemplateCustomizer.Target.LISTENER)) {
					template.setRetryPolicy(this.retryPolicy);
				}
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableRabbit
	protected static class EnableRabbitConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	protected static class NoEnableRabbitConfiguration {

	}

}
