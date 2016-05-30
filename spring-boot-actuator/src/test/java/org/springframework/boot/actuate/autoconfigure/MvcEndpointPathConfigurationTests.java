/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import liquibase.integration.spring.SpringLiquibase;
import org.flywaydb.core.Flyway;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.actuate.endpoint.AutoConfigurationReportEndpoint;
import org.springframework.boot.actuate.endpoint.BeansEndpoint;
import org.springframework.boot.actuate.endpoint.ConfigurationPropertiesReportEndpoint;
import org.springframework.boot.actuate.endpoint.DumpEndpoint;
import org.springframework.boot.actuate.endpoint.FlywayEndpoint;
import org.springframework.boot.actuate.endpoint.InfoEndpoint;
import org.springframework.boot.actuate.endpoint.LiquibaseEndpoint;
import org.springframework.boot.actuate.endpoint.RequestMappingEndpoint;
import org.springframework.boot.actuate.endpoint.ShutdownEndpoint;
import org.springframework.boot.actuate.endpoint.TraceEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.DocsMvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.EndpointMvcAdapter;
import org.springframework.boot.actuate.endpoint.mvc.EnvironmentMvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.HalJsonMvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.HealthMvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.JolokiaMvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.LogFileMvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.ManagementServletContext;
import org.springframework.boot.actuate.endpoint.mvc.MetricsMvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoints;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for configuring the path of an MVC endpoint.
 *
 * @author Andy Wilkinson
 */
@RunWith(Parameterized.class)
public class MvcEndpointPathConfigurationTests {

	private final String endpointName;

	private final Class<?> endpointClass;

	private AnnotationConfigWebApplicationContext context;

	@After
	public void cleanUp() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Parameters(name = "{0}")
	public static Object[] parameters() {
		return new Object[] { new Object[] { "actuator", HalJsonMvcEndpoint.class },
				new Object[] { "autoconfig", AutoConfigurationReportEndpoint.class },
				new Object[] { "beans", BeansEndpoint.class },
				new Object[] { "configprops",
						ConfigurationPropertiesReportEndpoint.class },
				new Object[] { "docs", DocsMvcEndpoint.class },
				new Object[] { "dump", DumpEndpoint.class },
				new Object[] { "env", EnvironmentMvcEndpoint.class },
				new Object[] { "flyway", FlywayEndpoint.class },
				new Object[] { "health", HealthMvcEndpoint.class },
				new Object[] { "info", InfoEndpoint.class },
				new Object[] { "jolokia", JolokiaMvcEndpoint.class },
				new Object[] { "liquibase", LiquibaseEndpoint.class },
				new Object[] { "logfile", LogFileMvcEndpoint.class },
				new Object[] { "mappings", RequestMappingEndpoint.class },
				new Object[] { "metrics", MetricsMvcEndpoint.class },
				new Object[] { "shutdown", ShutdownEndpoint.class },
				new Object[] { "trace", TraceEndpoint.class } };
	}

	public MvcEndpointPathConfigurationTests(String endpointName,
			Class<?> endpointClass) {
		this.endpointName = endpointName;
		this.endpointClass = endpointClass;
	}

	@Test
	public void pathCanBeConfigured() {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.register(TestConfiguration.class);
		this.context.setServletContext(new MockServletContext());
		EnvironmentTestUtils.addEnvironment(this.context,
				"endpoints." + this.endpointName + ".path" + ":/custom/path",
				"endpoints." + this.endpointName + ".enabled:true",
				"logging.file:target/test.log");
		this.context.refresh();
		assertThat(getConfiguredPath()).isEqualTo("/custom/path");
	}

	private String getConfiguredPath() {
		if (MvcEndpoint.class.isAssignableFrom(this.endpointClass)) {
			return ((MvcEndpoint) this.context.getBean(this.endpointClass)).getPath();
		}
		for (MvcEndpoint endpoint : this.context.getBean(MvcEndpoints.class)
				.getEndpoints()) {
			if (endpoint instanceof EndpointMvcAdapter && this.endpointClass
					.isInstance(((EndpointMvcAdapter) endpoint).getDelegate())) {
				return ((EndpointMvcAdapter) endpoint).getPath();
			}
		}
		throw new IllegalStateException(
				"Could not get configured path for " + this.endpointClass);
	}

	@Configuration
	@ImportAutoConfiguration({ EndpointAutoConfiguration.class,
			HttpMessageConvertersAutoConfiguration.class,
			ManagementServerPropertiesAutoConfiguration.class,
			ServerPropertiesAutoConfiguration.class,
			EndpointWebMvcAutoConfiguration.class, JolokiaAutoConfiguration.class,
			EndpointAutoConfiguration.class })

	protected static class TestConfiguration {

		@Bean
		public ConditionEvaluationReport conditionEvaluationReport(
				ConfigurableListableBeanFactory beanFactory) {
			return ConditionEvaluationReport.get(beanFactory);
		}

		@Bean
		public FlywayEndpoint flyway() {
			return new FlywayEndpoint(new Flyway());
		}

		@Bean
		public LiquibaseEndpoint liquibase() {
			return new LiquibaseEndpoint(new SpringLiquibase());
		}

		@Bean
		public DocsMvcEndpoint docs(ManagementServletContext managementServletContext) {
			return new DocsMvcEndpoint(managementServletContext);
		}

	}

}
