/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.endpoint.jmx;

import org.junit.Test;

import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.actuate.endpoint.AuditEventsEndpoint;
import org.springframework.boot.actuate.endpoint.jmx.AuditEventsJmxEndpointExtension;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JmxEndpointAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
public class JmxEndpointAutoConfigurationTests {

	@Test
	public void auditEventsJmxEndpointExtensionIsAutoConfigured() {
		jmxExtensionIsAutoConfigured(AuditEventsJmxEndpointExtension.class,
				AuditEventsEndpointConfiguration.class);
	}

	@Test
	public void auditEventsJmxEndpointExtensionCanBeDisabled() {
		jmxExtensionCanBeDisabled(AuditEventsJmxEndpointExtension.class, "auditevents",
				AuditEventsEndpointConfiguration.class);
	}

	private void jmxExtensionIsAutoConfigured(Class<?> jmxExtension, Class<?>... config) {
		contextRunner().withUserConfiguration(config)
				.run((context) -> assertThat(context).hasSingleBean(jmxExtension));
	}

	private void jmxExtensionCanBeDisabled(Class<?> jmxExtension, String id,
			Class<?>... config) {
		contextRunner().withPropertyValues("endpoints." + id + ".enabled=false")
				.run((context) -> assertThat(context).doesNotHaveBean(jmxExtension));
	}

	private ApplicationContextRunner contextRunner() {
		return new ApplicationContextRunner().withConfiguration(
				AutoConfigurations.of(JmxEndpointAutoConfiguration.class));
	}

	@Configuration
	static class AuditEventsEndpointConfiguration {

		@Bean
		public AuditEventsEndpoint auditEventsEndpoint() {
			return new AuditEventsEndpoint(mock(AuditEventRepository.class));
		}

	}

}
