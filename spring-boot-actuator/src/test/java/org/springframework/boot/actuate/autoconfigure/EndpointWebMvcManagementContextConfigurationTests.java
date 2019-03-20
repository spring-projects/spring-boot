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

package org.springframework.boot.actuate.autoconfigure;

import java.security.Principal;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.actuate.audit.InMemoryAuditEventRepository;
import org.springframework.boot.actuate.endpoint.EnvironmentEndpoint;
import org.springframework.boot.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.actuate.endpoint.LoggersEndpoint;
import org.springframework.boot.actuate.endpoint.MetricsEndpoint;
import org.springframework.boot.actuate.endpoint.ShutdownEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.AuditEventsMvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.EndpointHandlerMapping;
import org.springframework.boot.actuate.endpoint.mvc.EnvironmentMvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.HealthMvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.HeapdumpMvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.LogFileMvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.LoggersMvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.MetricsMvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpointSecurityInterceptor;
import org.springframework.boot.actuate.endpoint.mvc.ShutdownMvcEndpoint;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebClientAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EndpointWebMvcManagementContextConfiguration}.
 *
 * @author Madhura Bhave
 */
public class EndpointWebMvcManagementContextConfigurationTests {

	private AnnotationConfigWebApplicationContext context;

	@Before
	public void setup() {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
	}

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void endpointHandlerMapping() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"management.security.enabled=false",
				"management.security.roles=my-role,your-role");
		this.context.register(TestEndpointConfiguration.class);
		this.context.refresh();
		EndpointHandlerMapping mapping = this.context.getBean("endpointHandlerMapping",
				EndpointHandlerMapping.class);
		assertThat(mapping.getPrefix()).isEmpty();
		MvcEndpointSecurityInterceptor securityInterceptor = (MvcEndpointSecurityInterceptor) ReflectionTestUtils
				.getField(mapping, "securityInterceptor");
		Object secure = ReflectionTestUtils.getField(securityInterceptor, "secure");
		List<String> roles = getRoles(securityInterceptor);
		assertThat(secure).isEqualTo(false);
		assertThat(roles).containsExactly("my-role", "your-role");
	}

	@Test
	public void healthMvcEndpointIsConditionalOnMissingBean() throws Exception {
		this.context.register(HealthConfiguration.class, TestEndpointConfiguration.class);
		this.context.refresh();
		HealthMvcEndpoint mvcEndpoint = this.context.getBean(HealthMvcEndpoint.class);
		assertThat(mvcEndpoint).isInstanceOf(TestHealthMvcEndpoint.class);
	}

	@Test
	public void envMvcEndpointIsConditionalOnMissingBean() throws Exception {
		this.context.register(EnvConfiguration.class, TestEndpointConfiguration.class);
		this.context.refresh();
		EnvironmentMvcEndpoint mvcEndpoint = this.context
				.getBean(EnvironmentMvcEndpoint.class);
		assertThat(mvcEndpoint).isInstanceOf(TestEnvMvcEndpoint.class);
	}

	@Test
	public void metricsMvcEndpointIsConditionalOnMissingBean() throws Exception {
		this.context.register(MetricsConfiguration.class,
				TestEndpointConfiguration.class);
		this.context.refresh();
		MetricsMvcEndpoint mvcEndpoint = this.context.getBean(MetricsMvcEndpoint.class);
		assertThat(mvcEndpoint).isInstanceOf(TestMetricsMvcEndpoint.class);
	}

	@Test
	public void logFileMvcEndpointIsConditionalOnMissingBean() throws Exception {
		this.context.register(LogFileConfiguration.class,
				TestEndpointConfiguration.class);
		this.context.refresh();
		LogFileMvcEndpoint mvcEndpoint = this.context.getBean(LogFileMvcEndpoint.class);
		assertThat(mvcEndpoint).isInstanceOf(TestLogFileMvcEndpoint.class);
	}

	@Test
	public void shutdownEndpointIsConditionalOnMissingBean() throws Exception {
		this.context.register(ShutdownConfiguration.class,
				TestEndpointConfiguration.class);
		this.context.refresh();
		ShutdownMvcEndpoint mvcEndpoint = this.context.getBean(ShutdownMvcEndpoint.class);
		assertThat(mvcEndpoint).isInstanceOf(TestShutdownMvcEndpoint.class);
	}

	@Test
	public void auditEventsMvcEndpointIsConditionalOnMissingBean() throws Exception {
		this.context.register(AuditEventsConfiguration.class,
				TestEndpointConfiguration.class);
		this.context.refresh();
		AuditEventsMvcEndpoint mvcEndpoint = this.context
				.getBean(AuditEventsMvcEndpoint.class);
		assertThat(mvcEndpoint).isInstanceOf(TestAuditEventsMvcEndpoint.class);
	}

	@Test
	public void loggersMvcEndpointIsConditionalOnMissingBean() throws Exception {
		this.context.register(LoggersConfiguration.class,
				TestEndpointConfiguration.class);
		this.context.refresh();
		LoggersMvcEndpoint mvcEndpoint = this.context.getBean(LoggersMvcEndpoint.class);
		assertThat(mvcEndpoint).isInstanceOf(TestLoggersMvcEndpoint.class);
	}

	@Test
	public void heapdumpMvcEndpointIsConditionalOnMissingBean() throws Exception {
		this.context.register(HeapdumpConfiguration.class,
				TestEndpointConfiguration.class);
		this.context.refresh();
		HeapdumpMvcEndpoint mvcEndpoint = this.context.getBean(HeapdumpMvcEndpoint.class);
		assertThat(mvcEndpoint).isInstanceOf(TestHeapdumpMvcEndpoint.class);
	}

	@SuppressWarnings("unchecked")
	private List<String> getRoles(MvcEndpointSecurityInterceptor securityInterceptor) {
		return (List<String>) ReflectionTestUtils.getField(securityInterceptor, "roles");
	}

	@Configuration
	@ImportAutoConfiguration({ SecurityAutoConfiguration.class,
			WebMvcAutoConfiguration.class, JacksonAutoConfiguration.class,
			HttpMessageConvertersAutoConfiguration.class, EndpointAutoConfiguration.class,
			EndpointWebMvcAutoConfiguration.class,
			ManagementServerPropertiesAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class, WebClientAutoConfiguration.class,
			EndpointWebMvcManagementContextConfiguration.class })
	static class TestEndpointConfiguration {

	}

	@Configuration
	static class HealthConfiguration {

		@Bean
		public HealthMvcEndpoint testHealthMvcEndpoint(HealthEndpoint endpoint) {
			return new TestHealthMvcEndpoint(endpoint);
		}

	}

	@Configuration
	static class EnvConfiguration {

		@Bean
		public EnvironmentMvcEndpoint testEnvironmentMvcEndpoint(
				EnvironmentEndpoint endpoint) {
			return new TestEnvMvcEndpoint(endpoint);
		}

	}

	@Configuration
	static class MetricsConfiguration {

		@Bean
		public MetricsMvcEndpoint testMetricsMvcEndpoint(MetricsEndpoint endpoint) {
			return new TestMetricsMvcEndpoint(endpoint);
		}

	}

	@Configuration
	static class LoggersConfiguration {

		@Bean
		public LoggersMvcEndpoint testLoggersMvcEndpoint(LoggersEndpoint endpoint) {
			return new TestLoggersMvcEndpoint(endpoint);
		}

		@Bean
		LoggersEndpoint loggersEndpoint() {
			return new LoggersEndpoint(new LoggingSystem() {
				@Override
				public void beforeInitialize() {

				}
			});
		}

	}

	@Configuration
	static class LogFileConfiguration {

		@Bean
		public LogFileMvcEndpoint testLogFileMvcEndpoint() {
			return new TestLogFileMvcEndpoint();
		}

	}

	@Configuration
	static class AuditEventsConfiguration {

		@Bean
		public AuditEventRepository repository() {
			return new TestAuditEventRepository();
		}

		@Bean
		public AuditEventsMvcEndpoint testAuditEventsMvcEndpoint(
				AuditEventRepository repository) {
			return new TestAuditEventsMvcEndpoint(repository);
		}

	}

	static class TestAuditEventRepository extends InMemoryAuditEventRepository {

	}

	@Configuration
	static class HeapdumpConfiguration {

		@Bean
		public HeapdumpMvcEndpoint testHeapdumpMvcEndpoint() {
			return new TestHeapdumpMvcEndpoint();
		}

	}

	@Configuration
	static class ShutdownConfiguration {

		@Bean
		public ShutdownMvcEndpoint testShutdownMvcEndpoint(ShutdownEndpoint endpoint) {
			return new TestShutdownMvcEndpoint(endpoint);
		}

	}

	static class TestHealthMvcEndpoint extends HealthMvcEndpoint {

		TestHealthMvcEndpoint(HealthEndpoint delegate) {
			super(delegate);
		}

		@Override
		protected boolean exposeHealthDetails(HttpServletRequest request,
				Principal principal) {
			return true;
		}

	}

	static class TestEnvMvcEndpoint extends EnvironmentMvcEndpoint {

		TestEnvMvcEndpoint(EnvironmentEndpoint delegate) {
			super(delegate);
		}

	}

	static class TestLoggersMvcEndpoint extends LoggersMvcEndpoint {

		TestLoggersMvcEndpoint(LoggersEndpoint delegate) {
			super(delegate);
		}

	}

	static class TestHeapdumpMvcEndpoint extends HeapdumpMvcEndpoint {

	}

	static class TestLogFileMvcEndpoint extends LogFileMvcEndpoint {

	}

	static class TestMetricsMvcEndpoint extends MetricsMvcEndpoint {

		TestMetricsMvcEndpoint(MetricsEndpoint delegate) {
			super(delegate);
		}

	}

	static class TestAuditEventsMvcEndpoint extends AuditEventsMvcEndpoint {

		TestAuditEventsMvcEndpoint(AuditEventRepository auditEventRepository) {
			super(auditEventRepository);
		}

	}

	static class TestShutdownMvcEndpoint extends ShutdownMvcEndpoint {

		TestShutdownMvcEndpoint(ShutdownEndpoint delegate) {
			super(delegate);
		}

	}

}
