/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.amqp.rabbitmq.autoconfigure;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javax.net.ssl.SSLEngine;

import com.rabbitmq.client.amqp.ConnectionSettings.TlsSettings;
import com.rabbitmq.client.amqp.Environment;
import com.rabbitmq.client.amqp.impl.AmqpEnvironmentBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.config.ContainerCustomizer;
import org.springframework.amqp.rabbitmq.client.AmqpConnectionFactory;
import org.springframework.amqp.rabbitmq.client.RabbitAmqpAdmin;
import org.springframework.amqp.rabbitmq.client.RabbitAmqpTemplate;
import org.springframework.amqp.rabbitmq.client.config.RabbitAmqpListenerContainerFactory;
import org.springframework.amqp.rabbitmq.client.listener.RabbitAmqpListenerContainer;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.amqp.rabbitmq.autoconfigure.AmqpRabbitConnectionDetails.Address;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleKey;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.boot.ssl.SslStoreBundle;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AmqpRabbitAutoConfiguration}.
 *
 * @author Eddú Meléndez
 */
class AmqpRabbitAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(AmqpRabbitAutoConfiguration.class));

	@Test
	void testDefaultRabbitAmqpConfiguration() {
		this.contextRunner.run((context) -> {
			RabbitAmqpListenerContainerFactory listenerContainerFactory = context
				.getBean(RabbitAmqpListenerContainerFactory.class);
			AmqpConnectionFactory connectionFactory = context.getBean(AmqpConnectionFactory.class);
			RabbitAmqpTemplate rabbitAmqpTemplate = context.getBean(RabbitAmqpTemplate.class);
			RabbitAmqpAdmin rabbitAmqpAdmin = context.getBean(RabbitAmqpAdmin.class);

			assertThat(listenerContainerFactory).isNotNull();
			assertThat(listenerContainerFactory).extracting("containerCustomizer").isNull();
			assertThat(connectionFactory).isNotNull();
			assertThat(rabbitAmqpTemplate).isNotNull();
			assertThat(rabbitAmqpAdmin).isNotNull();
		});
	}

	@Test
	void whenMultipleRabbitAmqpTemplateCustomizersAreDefinedThenTheyAreCalledInOrder() {
		this.contextRunner.withUserConfiguration(MultipleRabbitAmqpTemplateCustomizersConfiguration.class)
			.run((context) -> {
				RabbitAmqpTemplateCustomizer firstCustomizer = context.getBean("firstCustomizer",
						RabbitAmqpTemplateCustomizer.class);
				RabbitAmqpTemplateCustomizer secondCustomizer = context.getBean("secondCustomizer",
						RabbitAmqpTemplateCustomizer.class);
				InOrder inOrder = inOrder(firstCustomizer, secondCustomizer);
				RabbitAmqpTemplate template = context.getBean(RabbitAmqpTemplate.class);
				then(firstCustomizer).should(inOrder).customize(template);
				then(secondCustomizer).should(inOrder).customize(template);
				inOrder.verifyNoMoreInteractions();
			});
	}

	@Test
	void testListenerContainerFactoryWithContainerCustomizer() {
		this.contextRunner.withUserConfiguration(AmqpContainerCustomizerConfiguration.class).run((context) -> {
			RabbitAmqpListenerContainerFactory listenerContainerFactory = context
				.getBean(RabbitAmqpListenerContainerFactory.class);

			assertThat(listenerContainerFactory).isNotNull();
			assertThat(listenerContainerFactory).extracting("containerCustomizer").isNotNull();
		});
	}

	@Test
	void autoConfigurationConfiguresConnectionDetailsWithDefaultValues() {
		this.contextRunner.run((context) -> {
			AmqpRabbitConnectionDetails connectionDetails = context.getBean(AmqpRabbitConnectionDetails.class);
			AmqpRabbitProperties properties = new AmqpRabbitProperties();
			assertThat(connectionDetails.getAddress().host()).isEqualTo(properties.getHost());
			assertThat(connectionDetails.getAddress().port()).isEqualTo(5672);
			assertThat(connectionDetails.getUsername()).isEqualTo(properties.getUsername());
			assertThat(connectionDetails.getPassword()).isEqualTo(properties.getPassword());
			assertThat(connectionDetails.getVirtualHost()).isNull();
		});
	}

	@Test
	void autoConfigurationConfiguresConnectionDetailsWithPropertyOverrides() {
		this.contextRunner
			.withPropertyValues("spring.amqp.rabbitmq.host=custom.host", "spring.amqp.rabbitmq.port=1234",
					"spring.amqp.rabbitmq.username=user", "spring.amqp.rabbitmq.password=secret")
			.run((context) -> {
				AmqpRabbitConnectionDetails connectionDetails = context.getBean(AmqpRabbitConnectionDetails.class);
				assertThat(connectionDetails.getAddress().host()).isEqualTo("custom.host");
				assertThat(connectionDetails.getAddress().port()).isEqualTo(1234);
				assertThat(connectionDetails.getUsername()).isEqualTo("user");
				assertThat(connectionDetails.getPassword()).isEqualTo("secret");
			});
	}

	@Test
	void autoConfigurationConfiguresEnvironmentWithSslBundleOptions() {
		SslOptions options = SslOptions.of(new String[] { "TLS_AES_128_GCM_SHA256" }, new String[] { "TLSv1.3" });
		SslBundle sslBundle = SslBundle.of(SslStoreBundle.NONE, SslBundleKey.NONE, options);
		SslBundles sslBundles = mock(SslBundles.class);
		given(sslBundles.getBundle("test-bundle")).willReturn(sslBundle);
		AtomicReference<TlsSettings<?>> tls = new AtomicReference<>();
		this.contextRunner.withPropertyValues("spring.amqp.rabbitmq.ssl.bundle=test-bundle")
			.withBean(SslBundles.class, () -> sslBundles)
			.withBean(AmqpEnvironmentCustomizer.class, () -> (builder) -> tls.set(builder.connectionSettings().tls()))
			.run((context) -> {
				assertThat(tls.get()).hasFieldOrPropertyWithValue("ciphers", new String[] { "TLS_AES_128_GCM_SHA256" });
				@SuppressWarnings("unchecked")
				Consumer<SSLEngine> engineCustomizer = (Consumer<SSLEngine>) ReflectionTestUtils.getField(tls.get(),
						"sslEngineCustomizer");
				assertThat(engineCustomizer).isNotNull();
				SSLEngine engine = mock(SSLEngine.class);
				engineCustomizer.accept(engine);
				then(engine).should().setEnabledProtocols(new String[] { "TLSv1.3" });
			});
	}

	@Test
	void autoConfigurationBacksOffWhenUserProvidesConnectionDetails() {
		AmqpRabbitConnectionDetails connectionDetails = mock(AmqpRabbitConnectionDetails.class);
		given(connectionDetails.getAddress()).willReturn(new Address("localhost", 5672));
		this.contextRunner.withBean(AmqpRabbitConnectionDetails.class, () -> connectionDetails).run((context) -> {
			assertThat(context).hasSingleBean(AmqpRabbitConnectionDetails.class);
			assertThat(context.getBean(AmqpRabbitConnectionDetails.class)).isSameAs(connectionDetails);
		});
	}

	@Test
	void autoConfigurationBacksOffWhenUserProvidesEnvironment() {
		Environment environment = mock(Environment.class);
		this.contextRunner.withBean(Environment.class, () -> environment).run((context) -> {
			assertThat(context).hasSingleBean(Environment.class);
			assertThat(context.getBean(Environment.class)).isSameAs(environment);
		});
	}

	@Test
	void autoConfigurationBacksOffWhenUserProvidesConnectionFactory() {
		AmqpConnectionFactory connectionFactory = mock(AmqpConnectionFactory.class);
		this.contextRunner.withBean(AmqpConnectionFactory.class, () -> connectionFactory).run((context) -> {
			assertThat(context).hasSingleBean(AmqpConnectionFactory.class);
			assertThat(context.getBean(AmqpConnectionFactory.class)).isSameAs(connectionFactory);
		});
	}

	@Test
	void autoConfigurationBacksOffWhenUserProvidesRabbitAmqpTemplate() {
		RabbitAmqpTemplate template = mock(RabbitAmqpTemplate.class);
		this.contextRunner.withBean(RabbitAmqpTemplate.class, () -> template).run((context) -> {
			assertThat(context).hasSingleBean(RabbitAmqpTemplate.class);
			assertThat(context.getBean(RabbitAmqpTemplate.class)).isSameAs(template);
		});
	}

	@Test
	void autoConfigurationBacksOffWhenUserProvidesRabbitAmqpAdmin() {
		RabbitAmqpAdmin admin = mock(RabbitAmqpAdmin.class);
		this.contextRunner.withBean(RabbitAmqpAdmin.class, () -> admin).run((context) -> {
			assertThat(context).hasSingleBean(RabbitAmqpAdmin.class);
			assertThat(context.getBean(RabbitAmqpAdmin.class)).isSameAs(admin);
		});
	}

	@Test
	void autoConfigurationBacksOffWhenUserProvidesRabbitListenerContainerFactory() {
		RabbitAmqpListenerContainerFactory factory = mock(RabbitAmqpListenerContainerFactory.class);
		this.contextRunner
			.withBean("rabbitListenerContainerFactory", RabbitAmqpListenerContainerFactory.class, () -> factory)
			.run((context) -> {
				assertThat(context).hasSingleBean(RabbitAmqpListenerContainerFactory.class);
				assertThat(context.getBean(RabbitAmqpListenerContainerFactory.class)).isSameAs(factory);
			});
	}

	@Test
	void autoConfigurationAppliesEnvironmentCustomizer() {
		AmqpEnvironmentCustomizer customizer = mock(AmqpEnvironmentCustomizer.class);
		this.contextRunner.withBean(AmqpEnvironmentCustomizer.class, () -> customizer)
			.run((context) -> then(customizer).should().customize(any(AmqpEnvironmentBuilder.class)));
	}

	@Test
	void autoConfigurationConfiguresConnectionDetailsWithSslBundle() {
		SslBundle sslBundle = SslBundle.of(SslStoreBundle.NONE);
		SslBundles sslBundles = mock(SslBundles.class);
		given(sslBundles.getBundle("test-bundle")).willReturn(sslBundle);
		this.contextRunner.withPropertyValues("spring.amqp.rabbitmq.ssl.bundle=test-bundle")
			.withBean(SslBundles.class, () -> sslBundles)
			.run((context) -> {
				AmqpRabbitConnectionDetails connectionDetails = context.getBean(AmqpRabbitConnectionDetails.class);
				assertThat(connectionDetails.getSslBundle()).isSameAs(sslBundle);
			});
	}

	@Test
	void autoConfigurationConfiguresRabbitAmqpTemplateWithMessageConverter() {
		this.contextRunner.withUserConfiguration(CustomMessageConverterConfiguration.class).run((context) -> {
			MessageConverter expected = context.getBean(MessageConverter.class);
			RabbitAmqpTemplate template = context.getBean(RabbitAmqpTemplate.class);
			assertThat(template).hasFieldOrPropertyWithValue("messageConverter", expected);
		});
	}

	@Test
	void autoConfigurationConfiguresRabbitAmqpTemplateWithTemplateProperties() {
		this.contextRunner
			.withPropertyValues("spring.amqp.rabbitmq.template.exchange=my-exchange",
					"spring.amqp.rabbitmq.template.routing-key=my-routing-key",
					"spring.amqp.rabbitmq.template.default-receive-queue=my-queue")
			.run((context) -> {
				RabbitAmqpTemplate template = context.getBean(RabbitAmqpTemplate.class);
				assertThat(template).hasFieldOrPropertyWithValue("defaultExchange", "my-exchange");
				assertThat(template).hasFieldOrPropertyWithValue("defaultRoutingKey", "my-routing-key");
				assertThat(template).hasFieldOrPropertyWithValue("defaultReceiveQueue", "my-queue");
			});
	}

	@Test
	void autoConfigurationDoesNotConfigureRabbitAmqpTemplateExchangeAndRoutingKeyByDefault() {
		this.contextRunner
			.withBean(RabbitAmqpTemplateCustomizer.class, () -> (template) -> template.setQueue("test.queue"))
			.run((context) -> {
				RabbitAmqpTemplate template = context.getBean(RabbitAmqpTemplate.class);
				assertThat(template).hasFieldOrPropertyWithValue("defaultQueue", "test.queue");
				assertThat(template).hasFieldOrPropertyWithValue("defaultExchange", null);
				assertThat(template).hasFieldOrPropertyWithValue("defaultRoutingKey", null);
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class MultipleRabbitAmqpTemplateCustomizersConfiguration {

		@Bean
		@Order(Ordered.LOWEST_PRECEDENCE)
		RabbitAmqpTemplateCustomizer secondCustomizer() {
			return mock(RabbitAmqpTemplateCustomizer.class);
		}

		@Bean
		@Order(0)
		RabbitAmqpTemplateCustomizer firstCustomizer() {
			return mock(RabbitAmqpTemplateCustomizer.class);
		}

	}

	@Import(TestListener.class)
	@Configuration(proxyBeanMethods = false)
	static class AmqpContainerCustomizerConfiguration {

		@Bean
		@SuppressWarnings("unchecked")
		ContainerCustomizer<RabbitAmqpListenerContainer> customizer() {
			return mock(ContainerCustomizer.class);
		}

	}

	@Configuration
	static class CustomMessageConverterConfiguration {

		@Bean
		MessageConverter messageConverter() {
			return new MessageConverter() {

				@Override
				public Message toMessage(Object object, MessageProperties messageProperties)
						throws MessageConversionException {
					return new Message(object.toString().getBytes());
				}

				@Override
				public Object fromMessage(Message message) throws MessageConversionException {
					return new String(message.getBody());
				}

			};
		}

	}

	static class TestListener {

		@RabbitListener(queues = "test", autoStartup = "false")
		void listen(String in) {
		}

	}

}
