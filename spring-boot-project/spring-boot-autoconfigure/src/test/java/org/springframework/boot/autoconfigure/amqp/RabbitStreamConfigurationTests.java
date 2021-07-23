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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.config.ContainerCustomizer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.rabbit.stream.config.StreamRabbitListenerContainerFactory;
import org.springframework.rabbit.stream.listener.ConsumerCustomizer;
import org.springframework.rabbit.stream.listener.StreamListenerContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RabbitStreamConfiguration}.
 *
 * @author Gary Russell
 */
class RabbitStreamConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(RabbitAutoConfiguration.class));

	@Test
	void testContainerType() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.withPropertyValues("spring.rabbitmq.listener.type:stream",
						"spring.rabbitmq.listener.stream.native-listener:true")
				.run((context) -> {
					RabbitListenerEndpointRegistry registry = context.getBean(RabbitListenerEndpointRegistry.class);
					assertThat(registry.getListenerContainer("test")).isInstanceOf(StreamListenerContainer.class);
					assertThat(new DirectFieldAccessor(registry.getListenerContainer("test"))
							.getPropertyValue("consumerCustomizer")).isNotNull();
					assertThat(new DirectFieldAccessor(context.getBean(StreamRabbitListenerContainerFactory.class))
							.getPropertyValue("nativeListener")).isEqualTo(Boolean.TRUE);
					assertThat(context.getBean(TestConfiguration.class).containerCustomizerCalled).isTrue();
				});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableRabbit
	static class TestConfiguration {

		boolean containerCustomizerCalled;

		@RabbitListener(id = "test", queues = "stream", autoStartup = "false")
		void listen(String in) {
		}

		@Bean
		ConsumerCustomizer consumerCustomizer() {
			return (id, consumer) -> {
			};
		}

		@Bean
		ContainerCustomizer<StreamListenerContainer> containerCustomizer() {
			return (container) -> this.containerCustomizerCalled = true;
		}

	}

}
