/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.amqp.autoconfigure;

import java.time.Duration;
import java.util.List;

import com.rabbitmq.stream.BackOffDelayPolicy;
import com.rabbitmq.stream.Codec;
import com.rabbitmq.stream.Environment;
import com.rabbitmq.stream.EnvironmentBuilder;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.amqp.core.MessageListenerContainer;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.config.ContainerCustomizer;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.rabbit.stream.config.StreamRabbitListenerContainerFactory;
import org.springframework.rabbit.stream.listener.ConsumerCustomizer;
import org.springframework.rabbit.stream.listener.StreamListenerContainer;
import org.springframework.rabbit.stream.micrometer.RabbitStreamListenerObservation.DefaultRabbitStreamListenerObservationConvention;
import org.springframework.rabbit.stream.micrometer.RabbitStreamListenerObservationConvention;
import org.springframework.rabbit.stream.micrometer.RabbitStreamTemplateObservation.DefaultRabbitStreamTemplateObservationConvention;
import org.springframework.rabbit.stream.micrometer.RabbitStreamTemplateObservationConvention;
import org.springframework.rabbit.stream.producer.ProducerCustomizer;
import org.springframework.rabbit.stream.producer.RabbitStreamTemplate;
import org.springframework.rabbit.stream.support.converter.StreamMessageConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RabbitStreamConfiguration}.
 *
 * @author Gary Russell
 * @author Andy Wilkinson
 * @author Eddú Meléndez
 * @author Moritz Halbritter
 */
class RabbitStreamConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(RabbitAutoConfiguration.class));

	@Test
	@SuppressWarnings("unchecked")
	void whenListenerTypeIsStreamThenStreamListenerContainerAndEnvironmentAreAutoConfigured() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
			.withPropertyValues("spring.rabbitmq.listener.type:stream")
			.run((context) -> {
				RabbitListenerEndpointRegistry registry = context.getBean(RabbitListenerEndpointRegistry.class);
				MessageListenerContainer listenerContainer = registry.getListenerContainer("test");
				assertThat(listenerContainer).isInstanceOf(StreamListenerContainer.class);
				assertThat(listenerContainer).extracting("consumerCustomizer").isNotNull();
				assertThat(context.getBean(StreamRabbitListenerContainerFactory.class))
					.extracting("nativeListener", InstanceOfAssertFactories.BOOLEAN)
					.isFalse();
				then(context.getBean(ContainerCustomizer.class)).should().configure(listenerContainer);
				assertThat(context).hasSingleBean(Environment.class);
			});
	}

	@Test
	void whenNativeListenerIsEnabledThenContainerFactoryIsConfiguredToUseNativeListeners() {
		this.contextRunner
			.withPropertyValues("spring.rabbitmq.listener.type:stream",
					"spring.rabbitmq.listener.stream.native-listener:true")
			.run((context) -> assertThat(context.getBean(StreamRabbitListenerContainerFactory.class))
				.extracting("nativeListener", InstanceOfAssertFactories.BOOLEAN)
				.isTrue());
	}

	@Test
	void shouldConfigureObservations() {
		this.contextRunner
			.withPropertyValues("spring.rabbitmq.listener.type:stream",
					"spring.rabbitmq.listener.stream.observation-enabled:true")
			.run((context) -> assertThat(context.getBean(StreamRabbitListenerContainerFactory.class))
				.extracting("observationEnabled", InstanceOfAssertFactories.BOOLEAN)
				.isTrue());
	}

	@Test
	void shouldConfigureRabbitStreamListenerObservationConvention() {
		RabbitStreamListenerObservationConvention convention = new DefaultRabbitStreamListenerObservationConvention();
		this.contextRunner.withBean(RabbitStreamListenerObservationConvention.class, () -> convention)
			.withPropertyValues("spring.rabbitmq.listener.type:stream")
			.run((context) -> {
				StreamRabbitListenerContainerFactory containerFactory = context
					.getBean(StreamRabbitListenerContainerFactory.class);
				assertThat(containerFactory).hasFieldOrPropertyWithValue("streamListenerObservationConvention",
						convention);
			});
	}

	@Test
	void environmentIsAutoConfiguredByDefault() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(Environment.class));
	}

	@Test
	void whenCustomEnvironmentIsDefinedThenAutoConfiguredEnvironmentBacksOff() {
		this.contextRunner.withUserConfiguration(CustomEnvironmentConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(Environment.class);
			assertThat(context.getBean(Environment.class))
				.isSameAs(context.getBean(CustomEnvironmentConfiguration.class).environment);
		});
	}

	@Test
	void whenCustomMessageListenerContainerFactoryIsDefinedThenAutoConfiguredContainerFactoryBacksOff() {
		this.contextRunner.withUserConfiguration(CustomMessageListenerContainerFactoryConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(RabbitListenerContainerFactory.class);
				assertThat(context.getBean(RabbitListenerContainerFactory.class)).isSameAs(context
					.getBean(CustomMessageListenerContainerFactoryConfiguration.class).listenerContainerFactory);
			});
	}

	@Test
	void environmentUsesConnectionDetailsByDefault() {
		EnvironmentBuilder builder = mock(EnvironmentBuilder.class);
		RabbitProperties properties = new RabbitProperties();
		RabbitStreamConfiguration.configure(builder, properties,
				new TestRabbitConnectionDetails("guest", "guest", "vhost"));
		then(builder).should().port(5552);
		then(builder).should().host("localhost");
		then(builder).should().virtualHost("vhost");
		then(builder).should().lazyInitialization(true);
		then(builder).should().username("guest");
		then(builder).should().password("guest");
		then(builder).shouldHaveNoMoreInteractions();
	}

	@Test
	void whenStreamPortIsSetThenEnvironmentUsesCustomPort() {
		EnvironmentBuilder builder = mock(EnvironmentBuilder.class);
		RabbitProperties properties = new RabbitProperties();
		properties.getStream().setPort(5553);
		RabbitStreamConfiguration.configure(builder, properties,
				new TestRabbitConnectionDetails("guest", "guest", "vhost"));
		then(builder).should().port(5553);
	}

	@Test
	void whenStreamHostIsSetThenEnvironmentUsesCustomHost() {
		EnvironmentBuilder builder = mock(EnvironmentBuilder.class);
		RabbitProperties properties = new RabbitProperties();
		properties.getStream().setHost("stream.rabbit.example.com");
		RabbitStreamConfiguration.configure(builder, properties,
				new TestRabbitConnectionDetails("guest", "guest", "vhost"));
		then(builder).should().host("stream.rabbit.example.com");
	}

	@Test
	void whenStreamVirtualHostIsSetThenEnvironmentUsesCustomVirtualHost() {
		EnvironmentBuilder builder = mock(EnvironmentBuilder.class);
		RabbitProperties properties = new RabbitProperties();
		properties.getStream().setVirtualHost("stream-virtual-host");
		RabbitStreamConfiguration.configure(builder, properties,
				new TestRabbitConnectionDetails("guest", "guest", "vhost"));
		then(builder).should().virtualHost("stream-virtual-host");
	}

	@Test
	void whenStreamVirtualHostIsNotSetButDefaultVirtualHostIsSetThenEnvironmentUsesDefaultVirtualHost() {
		EnvironmentBuilder builder = mock(EnvironmentBuilder.class);
		RabbitProperties properties = new RabbitProperties();
		properties.setVirtualHost("properties-virtual-host");
		RabbitStreamConfiguration.configure(builder, properties,
				new TestRabbitConnectionDetails("guest", "guest", "default-virtual-host"));
		then(builder).should().virtualHost("default-virtual-host");
	}

	@Test
	void whenStreamCredentialsAreNotSetThenEnvironmentUsesConnectionDetailsCredentials() {
		EnvironmentBuilder builder = mock(EnvironmentBuilder.class);
		RabbitProperties properties = new RabbitProperties();
		properties.setUsername("alice");
		properties.setPassword("secret");
		RabbitStreamConfiguration.configure(builder, properties,
				new TestRabbitConnectionDetails("bob", "password", "vhost"));
		then(builder).should().username("bob");
		then(builder).should().password("password");
	}

	@Test
	void whenStreamCredentialsAreSetThenEnvironmentUsesStreamCredentials() {
		EnvironmentBuilder builder = mock(EnvironmentBuilder.class);
		RabbitProperties properties = new RabbitProperties();
		properties.setUsername("alice");
		properties.setPassword("secret");
		properties.getStream().setUsername("bob");
		properties.getStream().setPassword("confidential");
		RabbitStreamConfiguration.configure(builder, properties,
				new TestRabbitConnectionDetails("charlotte", "hidden", "vhost"));
		then(builder).should().username("bob");
		then(builder).should().password("confidential");
	}

	@Test
	void testDefaultRabbitStreamTemplateConfiguration() {
		this.contextRunner.withPropertyValues("spring.rabbitmq.stream.name:stream-test").run((context) -> {
			assertThat(context).hasSingleBean(RabbitStreamTemplate.class);
			assertThat(context.getBean(RabbitStreamTemplate.class)).hasFieldOrPropertyWithValue("streamName",
					"stream-test");
		});
	}

	@Test
	void testDefaultRabbitStreamTemplateConfigurationWithoutStreamName() {
		this.contextRunner.withPropertyValues("spring.rabbitmq.listener.type:stream")
			.run((context) -> assertThat(context).doesNotHaveBean(RabbitStreamTemplate.class));
	}

	@Test
	void testRabbitStreamTemplateConfigurationWithCustomMessageConverter() {
		this.contextRunner.withUserConfiguration(MessageConvertersConfiguration.class)
			.withPropertyValues("spring.rabbitmq.stream.name:stream-test")
			.run((context) -> {
				assertThat(context).hasSingleBean(RabbitStreamTemplate.class);
				RabbitStreamTemplate streamTemplate = context.getBean(RabbitStreamTemplate.class);
				assertThat(streamTemplate).hasFieldOrPropertyWithValue("streamName", "stream-test");
				assertThat(streamTemplate).extracting("messageConverter")
					.isSameAs(context.getBean(MessageConverter.class));
			});
	}

	@Test
	void testRabbitStreamTemplateConfigurationWithCustomStreamMessageConverter() {
		this.contextRunner
			.withBean("myStreamMessageConverter", StreamMessageConverter.class,
					() -> mock(StreamMessageConverter.class))
			.withPropertyValues("spring.rabbitmq.stream.name:stream-test")
			.run((context) -> {
				assertThat(context).hasSingleBean(RabbitStreamTemplate.class);
				assertThat(context.getBean(RabbitStreamTemplate.class)).extracting("messageConverter")
					.isSameAs(context.getBean("myStreamMessageConverter"));
			});
	}

	@Test
	void testRabbitStreamTemplateConfigurationWithCustomProducerCustomizer() {
		this.contextRunner
			.withBean("myProducerCustomizer", ProducerCustomizer.class, () -> mock(ProducerCustomizer.class))
			.withPropertyValues("spring.rabbitmq.stream.name:stream-test")
			.run((context) -> {
				assertThat(context).hasSingleBean(RabbitStreamTemplate.class);
				assertThat(context.getBean(RabbitStreamTemplate.class)).extracting("producerCustomizer")
					.isSameAs(context.getBean("myProducerCustomizer"));
			});
	}

	@Test
	void shouldConfigureRabbitStreamTemplateObservationConvention() {
		RabbitStreamTemplateObservationConvention convention = new DefaultRabbitStreamTemplateObservationConvention();
		this.contextRunner.withBean(RabbitStreamTemplateObservationConvention.class, () -> convention)
			.withPropertyValues("spring.rabbitmq.stream.name:stream-test")
			.run((context) -> {
				RabbitStreamTemplate rabbitStreamTemplate = context.getBean(RabbitStreamTemplate.class);
				assertThat(rabbitStreamTemplate).hasFieldOrPropertyWithValue("observationConvention", convention);
			});
	}

	@Test
	void environmentCreatedByBuilderCanBeCustomized() {
		this.contextRunner.withUserConfiguration(EnvironmentBuilderCustomizers.class).run((context) -> {
			Environment environment = context.getBean(Environment.class);
			assertThat(environment).extracting("codec")
				.isEqualTo(context.getBean(EnvironmentBuilderCustomizers.class).codec);
			assertThat(environment).extracting("recoveryBackOffDelayPolicy")
				.isEqualTo(context.getBean(EnvironmentBuilderCustomizers.class).recoveryBackOffDelayPolicy);
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		@RabbitListener(id = "test", queues = "stream", autoStartup = "false")
		void listen(String in) {
		}

		@Bean
		ConsumerCustomizer consumerCustomizer() {
			return mock(ConsumerCustomizer.class);
		}

		@Bean
		@SuppressWarnings("unchecked")
		ContainerCustomizer<StreamListenerContainer> containerCustomizer() {
			return mock(ContainerCustomizer.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomEnvironmentConfiguration {

		private final Environment environment = Environment.builder().lazyInitialization(true).build();

		@Bean
		Environment rabbitStreamEnvironment() {
			return this.environment;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomMessageListenerContainerFactoryConfiguration {

		@SuppressWarnings("rawtypes")
		private final RabbitListenerContainerFactory listenerContainerFactory = mock(
				RabbitListenerContainerFactory.class);

		@Bean
		@SuppressWarnings("unchecked")
		RabbitListenerContainerFactory<MessageListenerContainer> rabbitListenerContainerFactory() {
			return this.listenerContainerFactory;
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
	static class EnvironmentBuilderCustomizers {

		private final Codec codec = mock(Codec.class);

		private final BackOffDelayPolicy recoveryBackOffDelayPolicy = BackOffDelayPolicy.fixed(Duration.ofSeconds(5));

		@Bean
		@Order(1)
		EnvironmentBuilderCustomizer customizerA() {
			return (builder) -> builder.codec(this.codec);
		}

		@Bean
		@Order(0)
		EnvironmentBuilderCustomizer customizerB() {
			return (builder) -> builder.codec(mock(Codec.class))
				.recoveryBackOffDelayPolicy(this.recoveryBackOffDelayPolicy);
		}

	}

	private static final class TestRabbitConnectionDetails implements RabbitConnectionDetails {

		private final String username;

		private final String password;

		private final String virtualHost;

		private TestRabbitConnectionDetails(String username, String password, String virtualHost) {
			this.username = username;
			this.password = password;
			this.virtualHost = virtualHost;
		}

		@Override
		public String getUsername() {
			return this.username;
		}

		@Override
		public String getPassword() {
			return this.password;
		}

		@Override
		public String getVirtualHost() {
			return this.virtualHost;
		}

		@Override
		public List<Address> getAddresses() {
			throw new UnsupportedOperationException();
		}

	}

}
