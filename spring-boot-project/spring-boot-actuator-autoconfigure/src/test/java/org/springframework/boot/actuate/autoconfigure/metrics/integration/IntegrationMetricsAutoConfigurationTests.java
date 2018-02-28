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

package org.springframework.boot.actuate.autoconfigure.metrics.integration;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.integration.IntegrationAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.SubscribableChannel;

/**
 * Tests for {@link IntegrationMetricsAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Gary Russell
 */
public class IntegrationMetricsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(IntegrationAutoConfiguration.class))
			.withUserConfiguration(BaseConfiguration.class)
			.withPropertyValues("spring.jmx.enabled=false");

	@Test
	public void autoConfiguredIntegrationIsInstrumented() {
		this.contextRunner.run((context) -> {
			Message<?> message = MessageBuilder.withPayload("hello").build();
			SubscribableChannel channel = context.getBean("errorChannel",
					SubscribableChannel.class);
			channel.send(message);
			MeterRegistry registry = context.getBean(MeterRegistry.class);
			registry.get("spring.integration.send").tag("name", "errorChannel").timer();
		});
	}

	@Configuration
	static class BaseConfiguration {

		@Bean
		public SimpleMeterRegistry simpleMeterRegistry() {
			return new SimpleMeterRegistry();
		}

	}

}
