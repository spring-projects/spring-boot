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

package org.springframework.boot.autoconfigure.amqp;

import com.rabbitmq.stream.Environment;
import com.rabbitmq.stream.EnvironmentBuilder;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.config.ContainerCustomizer;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.rabbit.stream.config.StreamRabbitListenerContainerFactory;
import org.springframework.rabbit.stream.listener.ConsumerCustomizer;
import org.springframework.rabbit.stream.listener.StreamListenerContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests for {@link RabbitStreamConfiguration}.
 *
 * @author Gary Russell
 * @author Andy Wilkinson
 */
class RabbitStreamConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(RabbitAutoConfiguration.class));

	@Test
	@SuppressWarnings("unchecked")
	void whenListenerTypeIsStreamThenStreamListenerContainerAndEnvironmentAreAutoConfigured() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.withPropertyValues("spring.rabbitmq.listener.type:stream").run((context) -> {
					RabbitListenerEndpointRegistry registry = context.getBean(RabbitListenerEndpointRegistry.class);
					MessageListenerContainer listenerContainer = registry.getListenerContainer("test");
					assertThat(listenerContainer).isInstanceOf(StreamListenerContainer.class);
					assertThat(listenerContainer).extracting("consumerCustomizer").isNotNull();
					assertThat(context.getBean(StreamRabbitListenerContainerFactory.class))
							.extracting("nativeListener", InstanceOfAssertFactories.BOOLEAN).isFalse();
					verify(context.getBean(ContainerCustomizer.class)).configure(listenerContainer);
					assertThat(context).hasSingleBean(Environment.class);
				});
	}

	@Test
	void whenNativeListenerIsEnabledThenContainerFactoryIsConfiguredToUseNativeListeners() {
		this.contextRunner
				.withPropertyValues("spring.rabbitmq.listener.type:stream",
						"spring.rabbitmq.listener.stream.native-listener:true")
				.run((context) -> assertThat(context.getBean(StreamRabbitListenerContainerFactory.class))
						.extracting("nativeListener", InstanceOfAssertFactories.BOOLEAN).isTrue());
	}

	@Test
	void whenCustomEnvironmenIsDefinedThenAutoConfiguredEnvironmentBacksOff() {
		this.contextRunner.withUserConfiguration(CustomEnvironmentConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(Environment.class);
			assertThat(context.getBean(Environment.class))
					.isSameAs(context.getBean(CustomEnvironmentConfiguration.class).environment);
		});
	}

	@Test
	void whenCustomMessageListenerContainerIsDefinedThenAutoConfiguredContainerBacksOff() {
		this.contextRunner.withUserConfiguration(CustomMessageListenerContainerFactoryConfiguration.class)
				.run((context) -> {
					assertThat(context).hasSingleBean(RabbitListenerContainerFactory.class);
					assertThat(context.getBean(RabbitListenerContainerFactory.class)).isSameAs(context.getBean(
							CustomMessageListenerContainerFactoryConfiguration.class).listenerContainerFactory);
				});
	}

	@Test
	void environmentUsesPropertyDefaultsByDefault() {
		EnvironmentBuilder builder = mock(EnvironmentBuilder.class);
		RabbitProperties properties = new RabbitProperties();
		RabbitStreamConfiguration.configure(builder, properties);
		verify(builder).port(5552);
		verify(builder).host("localhost");
		verify(builder).lazyInitialization(true);
		verify(builder).username("guest");
		verify(builder).password("guest");
		verifyNoMoreInteractions(builder);
	}

	@Test
	void whenStreamPortIsSetThenEnvironmentUsesCustomPort() {
		EnvironmentBuilder builder = mock(EnvironmentBuilder.class);
		RabbitProperties properties = new RabbitProperties();
		properties.getStream().setPort(5553);
		RabbitStreamConfiguration.configure(builder, properties);
		verify(builder).port(5553);
	}

	@Test
	void whenStreamHostIsSetThenEnvironmentUsesCustomHost() {
		EnvironmentBuilder builder = mock(EnvironmentBuilder.class);
		RabbitProperties properties = new RabbitProperties();
		properties.getStream().setHost("stream.rabbit.example.com");
		RabbitStreamConfiguration.configure(builder, properties);
		verify(builder).host("stream.rabbit.example.com");
	}

	@Test
	void whenStreamCredentialsAreNotSetThenEnvironmentUsesRabbitCredentials() {
		EnvironmentBuilder builder = mock(EnvironmentBuilder.class);
		RabbitProperties properties = new RabbitProperties();
		properties.setUsername("alice");
		properties.setPassword("secret");
		RabbitStreamConfiguration.configure(builder, properties);
		verify(builder).username("alice");
		verify(builder).password("secret");
	}

	@Test
	void whenStreamCredentialsAreSetThenEnvironmentUsesStreamCredentials() {
		EnvironmentBuilder builder = mock(EnvironmentBuilder.class);
		RabbitProperties properties = new RabbitProperties();
		properties.setUsername("alice");
		properties.setPassword("secret");
		properties.getStream().setUsername("bob");
		properties.getStream().setPassword("confidential");
		RabbitStreamConfiguration.configure(builder, properties);
		verify(builder).username("bob");
		verify(builder).password("confidential");
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

}
