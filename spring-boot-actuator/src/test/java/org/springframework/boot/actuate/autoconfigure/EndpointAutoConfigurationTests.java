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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.sql.DataSource;

import liquibase.integration.spring.SpringLiquibase;
import org.flywaydb.core.Flyway;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Test;

import org.springframework.boot.actuate.endpoint.AutoConfigurationReportEndpoint;
import org.springframework.boot.actuate.endpoint.BeansEndpoint;
import org.springframework.boot.actuate.endpoint.DumpEndpoint;
import org.springframework.boot.actuate.endpoint.EnvironmentEndpoint;
import org.springframework.boot.actuate.endpoint.FlywayEndpoint;
import org.springframework.boot.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.actuate.endpoint.InfoEndpoint;
import org.springframework.boot.actuate.endpoint.LiquibaseEndpoint;
import org.springframework.boot.actuate.endpoint.MetricsEndpoint;
import org.springframework.boot.actuate.endpoint.PublicMetrics;
import org.springframework.boot.actuate.endpoint.RequestMappingEndpoint;
import org.springframework.boot.actuate.endpoint.ShutdownEndpoint;
import org.springframework.boot.actuate.endpoint.TraceEndpoint;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizerBeanPostProcessor;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.MockEmbeddedServletContainerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link EndpointAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Greg Turnquist
 * @author Christian Dupuis
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 */
public class EndpointAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void endpoints() throws Exception {
		load(EndpointAutoConfiguration.class);
		assertNotNull(this.context.getBean(BeansEndpoint.class));
		assertNotNull(this.context.getBean(DumpEndpoint.class));
		assertNotNull(this.context.getBean(EnvironmentEndpoint.class));
		assertNotNull(this.context.getBean(HealthEndpoint.class));
		assertNotNull(this.context.getBean(InfoEndpoint.class));
		assertNotNull(this.context.getBean(MetricsEndpoint.class));
		assertNotNull(this.context.getBean(ShutdownEndpoint.class));
		assertNotNull(this.context.getBean(TraceEndpoint.class));
		assertNotNull(this.context.getBean(RequestMappingEndpoint.class));
	}

	@Test
	public void healthEndpoint() {
		load(EmbeddedDataSourceConfiguration.class, EndpointAutoConfiguration.class,
				HealthIndicatorAutoConfiguration.class);
		HealthEndpoint bean = this.context.getBean(HealthEndpoint.class);
		assertNotNull(bean);
		Health result = bean.invoke();
		assertNotNull(result);
		assertTrue("Wrong result: " + result, result.getDetails().containsKey("db"));
	}

	@Test
	public void healthEndpointWithDefaultHealthIndicator() {
		load(EndpointAutoConfiguration.class, HealthIndicatorAutoConfiguration.class);
		HealthEndpoint bean = this.context.getBean(HealthEndpoint.class);
		assertNotNull(bean);
		Health result = bean.invoke();
		assertNotNull(result);
	}

	@Test
	public void metricEndpointsHasSystemMetricsByDefault() {
		load(PublicMetricsAutoConfiguration.class, EndpointAutoConfiguration.class);
		MetricsEndpoint endpoint = this.context.getBean(MetricsEndpoint.class);
		Map<String, Object> metrics = endpoint.invoke();
		assertTrue(metrics.containsKey("mem"));
		assertTrue(metrics.containsKey("heap.used"));
	}

	@Test
	public void metricEndpointCustomPublicMetrics() {
		load(CustomPublicMetricsConfig.class, PublicMetricsAutoConfiguration.class,
				EndpointAutoConfiguration.class);
		MetricsEndpoint endpoint = this.context.getBean(MetricsEndpoint.class);
		Map<String, Object> metrics = endpoint.invoke();

		// Custom metrics
		assertTrue(metrics.containsKey("foo"));

		// System metrics still available
		assertTrue(metrics.containsKey("mem"));
		assertTrue(metrics.containsKey("heap.used"));

	}

	@Test
	public void autoConfigurationAuditEndpoints() {
		load(EndpointAutoConfiguration.class, ConditionEvaluationReport.class);
		assertNotNull(this.context.getBean(AutoConfigurationReportEndpoint.class));
	}

	@Test
	public void testInfoEndpointConfiguration() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context, "info.foo:bar");
		this.context.register(EndpointAutoConfiguration.class);
		this.context.refresh();
		InfoEndpoint endpoint = this.context.getBean(InfoEndpoint.class);
		assertNotNull(endpoint);
		assertNotNull(endpoint.invoke().get("git"));
		assertEquals("bar", endpoint.invoke().get("foo"));
	}

	@Test
	public void testNoGitProperties() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.git.properties:classpath:nonexistent");
		this.context.register(EndpointAutoConfiguration.class);
		this.context.refresh();
		InfoEndpoint endpoint = this.context.getBean(InfoEndpoint.class);
		assertNotNull(endpoint);
		assertNull(endpoint.invoke().get("git"));
	}

	@Test
	public void testFlywayEndpoint() throws Exception {
		AnnotationConfigEmbeddedWebApplicationContext context = new AnnotationConfigEmbeddedWebApplicationContext();
		context.register(Config.class, WebMvcAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class, ManagementServerPropertiesAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				EmbeddedDataSourceConfiguration.class,
				FlywayAutoConfiguration.class, EndpointAutoConfiguration.class,
				EndpointWebMvcAutoConfiguration.class);
		context.refresh();

		assertEquals(1, context.getBeanNamesForType(FlywayEndpoint.class).length);

		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
		mockMvc.perform(MockMvcRequestBuilders.get("/flyway"))
				.andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("\"version\":\"1\"")))
				.andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(1)))
				.andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
	}

	@Test
	public void testFlywayEndpointDisabled() throws Exception {
		AnnotationConfigEmbeddedWebApplicationContext context = new AnnotationConfigEmbeddedWebApplicationContext();
		EnvironmentTestUtils.addEnvironment(context, "endpoints.flyway.enabled:false");
		context.register(Config.class, WebMvcAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class, ManagementServerPropertiesAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				EmbeddedDataSourceConfiguration.class,
				FlywayAutoConfiguration.class, EndpointAutoConfiguration.class,
				EndpointWebMvcAutoConfiguration.class);
		context.refresh();

		assertEquals(0, context.getBeanNamesForType(FlywayEndpoint.class).length);

		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
		mockMvc.perform(MockMvcRequestBuilders.get("/flyway"))
				.andExpect(MockMvcResultMatchers.status().is4xxClientError());
	}

	@Test
	public void testFlywayEndpointNewId() throws Exception {
		AnnotationConfigEmbeddedWebApplicationContext context = new AnnotationConfigEmbeddedWebApplicationContext();
		EnvironmentTestUtils.addEnvironment(context, "endpoints.flyway.id:changelog");
		context.register(Config.class, WebMvcAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class, ManagementServerPropertiesAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				EmbeddedDataSourceConfiguration.class,
				FlywayAutoConfiguration.class, EndpointAutoConfiguration.class,
				EndpointWebMvcAutoConfiguration.class);
		context.refresh();

		assertEquals(1, context.getBeanNamesForType(FlywayEndpoint.class).length);

		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
		mockMvc.perform(MockMvcRequestBuilders.get("/changelog"))
				.andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("\"version\":\"1\"")))
				.andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(1)))
				.andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
	}

	@Test
	public void testFlywayEndpointTwoFlywayBeans() throws Exception {
		AnnotationConfigEmbeddedWebApplicationContext context = new AnnotationConfigEmbeddedWebApplicationContext();
		context.register(Config.class, WebMvcAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class, ManagementServerPropertiesAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				EmbeddedDataSourceConfiguration.class,
				FlywayConfig.class,
				FlywayAutoConfiguration.class, EndpointAutoConfiguration.class,
				EndpointWebMvcAutoConfiguration.class);
		context.refresh();

		assertEquals(2, context.getBeanNamesForType(Flyway.class).length);
		assertEquals(1, context.getBeanNamesForType(FlywayEndpoint.class).length);

		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
		mockMvc.perform(MockMvcRequestBuilders.get("/flyway"))
				.andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("\"version\":\"1\"")))
				.andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(1)))
				.andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
	}

	@Test
	public void testLiquibaseEndpoint() throws Exception {
		AnnotationConfigEmbeddedWebApplicationContext context = new AnnotationConfigEmbeddedWebApplicationContext();
		context.register(Config.class, WebMvcAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class, ManagementServerPropertiesAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				EmbeddedDataSourceConfiguration.class,
				LiquibaseAutoConfiguration.class, EndpointAutoConfiguration.class,
				EndpointWebMvcAutoConfiguration.class);
		context.refresh();

		assertEquals(1, context.getBeanNamesForType(LiquibaseEndpoint.class).length);

		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
		mockMvc.perform(MockMvcRequestBuilders.get("/liquibase"))
				.andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("\"ID\":\"1\"")))
				.andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(1)))
				.andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
	}

	@Test
	public void testLiquibaseEndpointDisabled() throws Exception {
		AnnotationConfigEmbeddedWebApplicationContext context = new AnnotationConfigEmbeddedWebApplicationContext();
		EnvironmentTestUtils.addEnvironment(context, "endpoints.liquibase.enabled:false");
		context.register(Config.class, WebMvcAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class, ManagementServerPropertiesAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				EmbeddedDataSourceConfiguration.class,
				LiquibaseAutoConfiguration.class, EndpointAutoConfiguration.class,
				EndpointWebMvcAutoConfiguration.class);
		context.refresh();

		assertEquals(0, context.getBeanNamesForType(LiquibaseEndpoint.class).length);

		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
		mockMvc.perform(MockMvcRequestBuilders.get("/liquibase"))
				.andExpect(MockMvcResultMatchers.status().is4xxClientError());
	}

	@Test
	public void testLiquibaseEndpointNewId() throws Exception {
		AnnotationConfigEmbeddedWebApplicationContext context = new AnnotationConfigEmbeddedWebApplicationContext();
		EnvironmentTestUtils.addEnvironment(context, "endpoints.liquibase.id:changelog");
		context.register(Config.class, WebMvcAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class, ManagementServerPropertiesAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				EmbeddedDataSourceConfiguration.class,
				LiquibaseAutoConfiguration.class, EndpointAutoConfiguration.class,
				EndpointWebMvcAutoConfiguration.class);
		context.refresh();

		assertEquals(1, context.getBeanNamesForType(LiquibaseEndpoint.class).length);

		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
		mockMvc.perform(MockMvcRequestBuilders.get("/changelog"))
				.andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("\"ID\":\"1\"")))
				.andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(1)))
				.andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
	}

	@Test
	public void testLiquibaseEndpointTwoSpringLiquibaseBeans() throws Exception {
		AnnotationConfigEmbeddedWebApplicationContext context = new AnnotationConfigEmbeddedWebApplicationContext();
		context.register(Config.class, WebMvcAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class, ManagementServerPropertiesAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				EmbeddedDataSourceConfiguration.class,
				LiquibaseConfig.class,
				LiquibaseAutoConfiguration.class, EndpointAutoConfiguration.class,
				EndpointWebMvcAutoConfiguration.class);
		context.refresh();

		assertEquals(2, context.getBeanNamesForType(SpringLiquibase.class).length);
		assertEquals(1, context.getBeanNamesForType(LiquibaseEndpoint.class).length);

		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
		mockMvc.perform(MockMvcRequestBuilders.get("/liquibase"))
				.andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("\"ID\":\"1\"")))
				.andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(1)))
				.andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
	}

	private void load(Class<?>... config) {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(config);
		this.context.refresh();
	}

	@Configuration
	static class CustomPublicMetricsConfig {

		@Bean
		PublicMetrics customPublicMetrics() {
			return new PublicMetrics() {
				@Override
				public Collection<Metric<?>> metrics() {
					Metric<Integer> metric = new Metric<Integer>("foo", 1);
					return Collections.<Metric<?>>singleton(metric);
				}
			};
		}

	}

	@Configuration
	@EnableConfigurationProperties
	protected static class Config {

		protected static MockEmbeddedServletContainerFactory containerFactory = null;

		@Bean
		public EmbeddedServletContainerFactory containerFactory() {
			if (containerFactory == null) {
				containerFactory = new MockEmbeddedServletContainerFactory();
			}
			return containerFactory;
		}

		@Bean
		public EmbeddedServletContainerCustomizerBeanPostProcessor embeddedServletContainerCustomizerBeanPostProcessor() {
			return new EmbeddedServletContainerCustomizerBeanPostProcessor();
		}

	}

	static class DataSourceConfig {

		@Bean
		public DataSource customDataSource() {
			return DataSourceBuilder.create().url("jdbc:hsqldb:mem:changelogdbtest")
					.username("sa").build();
		}

		@Bean
		public DataSource customDataSource2() {
			return DataSourceBuilder.create().url("jdbc:hsqldb:mem:changelogdbtest2")
					.username("sa").build();
		}

	}

	@Configuration
	static class LiquibaseConfig extends DataSourceConfig {

		@Primary
		@Bean
		public SpringLiquibase customLiquibase() {
			SpringLiquibase liquibase = new SpringLiquibase();
			liquibase.setDataSource(customDataSource());
			liquibase.setChangeLog("classpath:/db/changelog/db.changelog-master.yaml");
			return liquibase;
		}

		@Bean
		public SpringLiquibase customLiquibase2() {
			SpringLiquibase liquibase = new SpringLiquibase();
			liquibase.setDataSource(customDataSource2());
			liquibase.setChangeLog("classpath:/db/changelog/db.changelog-master.yaml");
			return liquibase;
		}

	}

	@Configuration
	static class FlywayConfig extends DataSourceConfig {

		@Primary
		@Bean
		public Flyway customFlyway() {
			Flyway flyway = new Flyway();
			flyway.setDataSource(customDataSource());
			return flyway;
		}

		@Bean
		public Flyway customFlyway2() {
			Flyway flyway = new Flyway();
			flyway.setDataSource(customDataSource2());
			return flyway;
		}

	}

}
