/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.observation.jms;

import jakarta.jms.ConnectionFactory;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.core.JmsTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JmsTemplateObservationAutoConfiguration}.
 *
 * @author Brian Clozel
 */
class JmsTemplateObservationAutoConfigurationTests {

	ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(JmsAutoConfiguration.class, ObservationAutoConfiguration.class,
				JmsTemplateObservationAutoConfiguration.class))
		.withUserConfiguration(JmsConnectionConfiguration.class);

	@Test
	void shouldConfigureObservationRegistryOnTemplate() {
		this.contextRunner.run((context) -> {
			JmsTemplate jmsTemplate = context.getBean(JmsTemplate.class);
			assertThat(jmsTemplate).extracting("observationRegistry").isNotNull();
		});
	}

	@Test
	void shouldBackOffWhenMircrometerCoreIsNotPresent() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("io.micrometer.core")).run((context) -> {
			JmsTemplate jmsTemplate = context.getBean(JmsTemplate.class);
			assertThat(jmsTemplate).extracting("observationRegistry").isNull();
		});
	}

	static class JmsConnectionConfiguration {

		@Bean
		ConnectionFactory connectionFactory() {
			return mock(ConnectionFactory.class);
		}

	}

}
