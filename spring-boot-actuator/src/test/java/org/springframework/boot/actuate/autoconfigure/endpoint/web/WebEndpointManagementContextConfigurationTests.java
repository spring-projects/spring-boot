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
import org.springframework.boot.actuate.endpoint.web.HealthReactiveWebEndpointExtension;
import org.springframework.boot.actuate.endpoint.web.HealthWebEndpointExtension;
import org.springframework.boot.actuate.endpoint.web.HeapDumpWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.LogFileWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.StatusReactiveWebEndpointExtension;
import org.springframework.boot.actuate.endpoint.web.StatusWebEndpointExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthStatusHttpMapper;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
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
		WebApplicationContextRunner contextRunner = webContextRunner()
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
		WebApplicationContextRunner contextRunner = webContextRunner()
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
	public void reactiveHealthWebEndpointExtensionIsAutoConfigured() {
		reactiveWebContextRunner(HealthEndpointConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(HealthReactiveWebEndpointExtension.class);
			assertThat(context).doesNotHaveBean(HealthWebEndpointExtension.class);
		});

	}

	@Test
	public void reactiveHealthStatusMappingCanBeCustomized() {
		reactiveWebContextRunner(HealthEndpointConfiguration.class)
				.withPropertyValues("management.health.status.http-mapping.CUSTOM=500")
				.run((context) -> {
					HealthReactiveWebEndpointExtension extension = context
							.getBean(HealthReactiveWebEndpointExtension.class);
					Map<String, Integer> statusMappings = getStatusMapping(extension);
					assertThat(statusMappings).containsEntry("DOWN", 503);
					assertThat(statusMappings).containsEntry("OUT_OF_SERVICE", 503);
					assertThat(statusMappings).containsEntry("CUSTOM", 500);
				});
	}

	@Test
	public void reactiveHealthWebEndpointExtensionCanBeDisabled() {
		reactiveWebContextRunner(HealthEndpointConfiguration.class)
				.withPropertyValues("endpoints.health.enabled=false").run((context) -> {
					assertThat(context)
							.doesNotHaveBean(HealthReactiveWebEndpointExtension.class);
					assertThat(context).doesNotHaveBean(HealthWebEndpointExtension.class);
				});

	}

	@Test
	public void reactiveStatusWebEndpointExtensionIsAutoConfigured() {
		reactiveWebContextRunner(StatusEndpointConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(StatusReactiveWebEndpointExtension.class);
			assertThat(context).doesNotHaveBean(StatusWebEndpointExtension.class);
		});
	}

	@Test
	public void reactiveStatusMappingCanBeCustomized() {
		reactiveWebContextRunner(StatusEndpointConfiguration.class)
				.withPropertyValues("management.health.status.http-mapping.CUSTOM=500")
				.run((context) -> {
					StatusReactiveWebEndpointExtension extension = context
							.getBean(StatusReactiveWebEndpointExtension.class);
					Map<String, Integer> statusMappings = getStatusMapping(extension);
					assertThat(statusMappings).containsEntry("DOWN", 503);
					assertThat(statusMappings).containsEntry("OUT_OF_SERVICE", 503);
					assertThat(statusMappings).containsEntry("CUSTOM", 500);
				});
	}

	@Test
	public void reactiveStatusWebEndpointExtensionCanBeDisabled() {
		reactiveWebContextRunner(StatusEndpointConfiguration.class)
				.withPropertyValues("endpoints.status.enabled=false").run((context) -> {
					assertThat(context)
							.doesNotHaveBean(StatusReactiveWebEndpointExtension.class);
					assertThat(context).doesNotHaveBean(StatusWebEndpointExtension.class);
				});
	}

	@Test
	public void logFileWebEndpointIsAutoConfiguredWhenLoggingFileIsSet() {
		webContextRunner().withPropertyValues("logging.file:test.log").run(
				(context) -> assertThat(context.getBeansOfType(LogFileWebEndpoint.class))
						.hasSize(1));
	}

	@Test
	public void logFileWebEndpointIsAutoConfiguredWhenLoggingPathIsSet() {
		webContextRunner().withPropertyValues("logging.path:test/logs").run(
				(context) -> assertThat(context.getBeansOfType(LogFileWebEndpoint.class))
						.hasSize(1));
	}

	@Test
	public void logFileWebEndpointIsAutoConfiguredWhenExternalFileIsSet() {
		webContextRunner()
				.withPropertyValues("endpoints.logfile.external-file:external.log")
				.run((context) -> assertThat(
						context.getBeansOfType(LogFileWebEndpoint.class)).hasSize(1));
	}

	@Test
	public void logFileWebEndpointCanBeDisabled() {
		webContextRunner()
				.withPropertyValues("logging.file:test.log",
						"endpoints.logfile.enabled:false")
				.run((context) -> assertThat(context)
						.hasSingleBean(LogFileWebEndpoint.class));
	}

	private void beanIsAutoConfigured(Class<?> beanType, Class<?>... config) {
		webContextRunner().withPropertyValues("endpoints.default.web.enabled:true")
				.withUserConfiguration(config)
				.run((context) -> assertThat(context).hasSingleBean(beanType));
	}

	private ReactiveWebApplicationContextRunner reactiveWebContextRunner(
			Class<?>... config) {
		return reactiveWebContextRunner()
				.withPropertyValues("endpoints.default.web.enabled:true")
				.withUserConfiguration(config);
	}

	private void beanIsNotAutoConfiguredWhenEndpointIsDisabled(Class<?> webExtension,
			String id, Class<?>... config) {
		webContextRunner().withPropertyValues("endpoints." + id + ".enabled=false")
				.run((context) -> assertThat(context).doesNotHaveBean(webExtension));
	}

	private WebApplicationContextRunner webContextRunner() {
		return new WebApplicationContextRunner().withConfiguration(
				AutoConfigurations.of(WebEndpointManagementContextConfiguration.class));
	}

	private ReactiveWebApplicationContextRunner reactiveWebContextRunner() {
		return new ReactiveWebApplicationContextRunner().withConfiguration(
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
