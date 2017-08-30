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

package org.springframework.boot.actuate.autoconfigure.endpoint.web;

import java.util.Map;

import org.junit.Test;

import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.actuate.endpoint.AuditEventsEndpoint;
import org.springframework.boot.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.actuate.endpoint.StatusEndpoint;
import org.springframework.boot.actuate.endpoint.web.AuditEventsWebEndpointExtension;
import org.springframework.boot.actuate.endpoint.web.HealthWebEndpointExtension;
import org.springframework.boot.actuate.endpoint.web.HeapDumpWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.LogFileWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.StatusWebEndpointExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthStatusHttpMapper;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link WebEndpointManagementContextConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
public class WebEndpointManagementContextConfigurationTests {

	@Test
	public void heapDumpWebEndpointIsAutoConfigured() {
		beanIsAutoConfigured(HeapDumpWebEndpoint.class);
	}

	@Test
	public void heapDumpWebEndpointCanBeDisabled() {
		beanIsNotAutoConfiguredWhenEndpointIsDisabled(HeapDumpWebEndpoint.class,
				"heapdump");
	}

	@Test
	public void healthWebEndpointExtensionIsAutoConfigured() {
		beanIsAutoConfigured(HealthWebEndpointExtension.class,
				HealthEndpointConfiguration.class);
	}

	@Test
	public void healthStatusMappingCanBeCustomized() {
		ApplicationContextRunner contextRunner = contextRunner()
				.withPropertyValues("management.health.status.http-mapping.CUSTOM=500")
				.withUserConfiguration(HealthEndpointConfiguration.class);
		contextRunner.run((context) -> {
			HealthWebEndpointExtension extension = context
					.getBean(HealthWebEndpointExtension.class);
			Map<String, Integer> statusMappings = getStatusMapping(extension);
			assertThat(statusMappings).containsEntry("DOWN", 503);
			assertThat(statusMappings).containsEntry("OUT_OF_SERVICE", 503);
			assertThat(statusMappings).containsEntry("CUSTOM", 500);
		});
	}

	@Test
	public void healthWebEndpointExtensionCanBeDisabled() {
		beanIsNotAutoConfiguredWhenEndpointIsDisabled(HealthWebEndpointExtension.class,
				"health", HealthEndpointConfiguration.class);
	}

	@Test
	public void statusWebEndpointExtensionIsAutoConfigured() {
		beanIsAutoConfigured(StatusWebEndpointExtension.class,
				StatusEndpointConfiguration.class);
	}

	@Test
	public void statusMappingCanBeCustomized() {
		ApplicationContextRunner contextRunner = contextRunner()
				.withPropertyValues("management.health.status.http-mapping.CUSTOM=500")
				.withUserConfiguration(StatusEndpointConfiguration.class);
		contextRunner.run((context) -> {
			StatusWebEndpointExtension extension = context
					.getBean(StatusWebEndpointExtension.class);
			Map<String, Integer> statusMappings = getStatusMapping(extension);
			assertThat(statusMappings).containsEntry("DOWN", 503);
			assertThat(statusMappings).containsEntry("OUT_OF_SERVICE", 503);
			assertThat(statusMappings).containsEntry("CUSTOM", 500);
		});
	}

	@Test
	public void statusWebEndpointExtensionCanBeDisabled() {
		beanIsNotAutoConfiguredWhenEndpointIsDisabled(StatusWebEndpointExtension.class,
				"status", StatusEndpointConfiguration.class);
	}

	@Test
	public void auditEventsWebEndpointExtensionIsAutoConfigured() {
		beanIsAutoConfigured(AuditEventsWebEndpointExtension.class,
				AuditEventsEndpointConfiguration.class);
	}

	@Test
	public void auditEventsWebEndpointExtensionCanBeDisabled() {
		beanIsNotAutoConfiguredWhenEndpointIsDisabled(
				AuditEventsWebEndpointExtension.class, "auditevents",
				AuditEventsEndpointConfiguration.class);
	}

	@Test
	public void logFileWebEndpointIsAutoConfiguredWhenLoggingFileIsSet() {
		contextRunner().withPropertyValues("logging.file:test.log").run(
				(context) -> assertThat(context.getBeansOfType(LogFileWebEndpoint.class))
						.hasSize(1));
	}

	@Test
	public void logFileWebEndpointIsAutoConfiguredWhenLoggingPathIsSet() {
		contextRunner().withPropertyValues("logging.path:test/logs").run(
				(context) -> assertThat(context.getBeansOfType(LogFileWebEndpoint.class))
						.hasSize(1));
	}

	@Test
	public void logFileWebEndpointIsAutoConfiguredWhenExternalFileIsSet() {
		contextRunner().withPropertyValues("endpoints.logfile.external-file:external.log")
				.run((context) -> assertThat(
						context.getBeansOfType(LogFileWebEndpoint.class)).hasSize(1));
	}

	@Test
	public void logFileWebEndpointCanBeDisabled() {
		contextRunner()
				.withPropertyValues("logging.file:test.log",
						"endpoints.logfile.enabled:false")
				.run((context) -> assertThat(context)
						.hasSingleBean(LogFileWebEndpoint.class));
	}

	private void beanIsAutoConfigured(Class<?> beanType, Class<?>... config) {
		contextRunner().withPropertyValues("endpoints.default.web.enabled:true")
				.withUserConfiguration(config)
				.run((context) -> assertThat(context).hasSingleBean(beanType));
	}

	private void beanIsNotAutoConfiguredWhenEndpointIsDisabled(Class<?> webExtension,
			String id, Class<?>... config) {
		contextRunner().withPropertyValues("endpoints." + id + ".enabled=false")
				.run((context) -> assertThat(context).doesNotHaveBean(webExtension));
	}

	private ApplicationContextRunner contextRunner() {
		return new ApplicationContextRunner().withConfiguration(
				AutoConfigurations.of(WebEndpointManagementContextConfiguration.class));
	}

	private Map<String, Integer> getStatusMapping(Object extension) {
		return ((HealthStatusHttpMapper) ReflectionTestUtils.getField(extension,
				"statusHttpMapper")).getStatusMapping();
	}

	@Configuration
	static class HealthEndpointConfiguration {

		@Bean
		public HealthEndpoint healthEndpoint() {
			return new HealthEndpoint(() -> Health.up().build());
		}

	}

	@Configuration
	static class StatusEndpointConfiguration {

		@Bean
		public StatusEndpoint statusEndpoint() {
			return new StatusEndpoint(() -> Health.up().build());
		}

	}

	@Configuration
	static class AuditEventsEndpointConfiguration {

		@Bean
		public AuditEventsEndpoint auditEventsEndpoint() {
			return new AuditEventsEndpoint(mock(AuditEventRepository.class));
		}

	}

}
