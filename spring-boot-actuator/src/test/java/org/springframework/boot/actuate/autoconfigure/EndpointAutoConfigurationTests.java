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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import liquibase.integration.spring.SpringLiquibase;
import org.flywaydb.core.Flyway;
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
import org.springframework.boot.actuate.endpoint.LoggersEndpoint;
import org.springframework.boot.actuate.endpoint.MetricsEndpoint;
import org.springframework.boot.actuate.endpoint.PublicMetrics;
import org.springframework.boot.actuate.endpoint.RequestMappingEndpoint;
import org.springframework.boot.actuate.endpoint.ShutdownEndpoint;
import org.springframework.boot.actuate.endpoint.TraceEndpoint;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration;
import org.springframework.boot.autoconfigure.info.ProjectInfoProperties;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.bind.PropertySourcesBinder;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.validation.BindException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EndpointAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Greg Turnquist
 * @author Christian Dupuis
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @author Meang Akira Tanaka
 * @author Ben Hale
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
		load(CustomLoggingConfig.class, EndpointAutoConfiguration.class);
		assertThat(this.context.getBean(BeansEndpoint.class)).isNotNull();
		assertThat(this.context.getBean(DumpEndpoint.class)).isNotNull();
		assertThat(this.context.getBean(EnvironmentEndpoint.class)).isNotNull();
		assertThat(this.context.getBean(HealthEndpoint.class)).isNotNull();
		assertThat(this.context.getBean(InfoEndpoint.class)).isNotNull();
		assertThat(this.context.getBean(LoggersEndpoint.class)).isNotNull();
		assertThat(this.context.getBean(MetricsEndpoint.class)).isNotNull();
		assertThat(this.context.getBean(ShutdownEndpoint.class)).isNotNull();
		assertThat(this.context.getBean(TraceEndpoint.class)).isNotNull();
		assertThat(this.context.getBean(RequestMappingEndpoint.class)).isNotNull();
	}

	@Test
	public void healthEndpoint() {
		load(EmbeddedDataSourceConfiguration.class, EndpointAutoConfiguration.class,
				HealthIndicatorAutoConfiguration.class);
		HealthEndpoint bean = this.context.getBean(HealthEndpoint.class);
		assertThat(bean).isNotNull();
		Health result = bean.invoke();
		assertThat(result).isNotNull();
		assertThat(result.getDetails().containsKey("db")).isTrue();
	}

	@Test
	public void healthEndpointWithDefaultHealthIndicator() {
		load(EndpointAutoConfiguration.class, HealthIndicatorAutoConfiguration.class);
		HealthEndpoint bean = this.context.getBean(HealthEndpoint.class);
		assertThat(bean).isNotNull();
		Health result = bean.invoke();
		assertThat(result).isNotNull();
	}

	@Test
	public void loggersEndpointHasLoggers() throws Exception {
		load(CustomLoggingConfig.class, EndpointAutoConfiguration.class);
		LoggersEndpoint endpoint = this.context.getBean(LoggersEndpoint.class);
		Map<String, Object> result = endpoint.invoke();
		assertThat((Map<?, ?>) result.get("loggers")).size().isGreaterThan(0);
	}

	@Test
	public void metricEndpointsHasSystemMetricsByDefault() {
		load(PublicMetricsAutoConfiguration.class, EndpointAutoConfiguration.class);
		MetricsEndpoint endpoint = this.context.getBean(MetricsEndpoint.class);
		Map<String, Object> metrics = endpoint.invoke();
		assertThat(metrics.containsKey("mem")).isTrue();
		assertThat(metrics.containsKey("heap.used")).isTrue();
	}

	@Test
	public void metricEndpointCustomPublicMetrics() {
		load(CustomPublicMetricsConfig.class, PublicMetricsAutoConfiguration.class,
				EndpointAutoConfiguration.class);
		MetricsEndpoint endpoint = this.context.getBean(MetricsEndpoint.class);
		Map<String, Object> metrics = endpoint.invoke();

		// Custom metrics
		assertThat(metrics.containsKey("foo")).isTrue();

		// System metrics still available
		assertThat(metrics.containsKey("mem")).isTrue();
		assertThat(metrics.containsKey("heap.used")).isTrue();

	}

	@Test
	public void autoConfigurationAuditEndpoints() {
		load(EndpointAutoConfiguration.class, ConditionEvaluationReport.class);
		assertThat(this.context.getBean(AutoConfigurationReportEndpoint.class))
				.isNotNull();
	}

	@Test
	public void testInfoEndpoint() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context, "info.foo:bar");
		this.context.register(ProjectInfoAutoConfiguration.class,
				InfoContributorAutoConfiguration.class, EndpointAutoConfiguration.class);
		this.context.refresh();

		InfoEndpoint endpoint = this.context.getBean(InfoEndpoint.class);
		assertThat(endpoint).isNotNull();
		assertThat(endpoint.invoke().get("git")).isNotNull();
		assertThat(endpoint.invoke().get("foo")).isEqualTo("bar");
	}

	@Test
	public void testInfoEndpointNoGitProperties() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.info.git.location:classpath:nonexistent");
		this.context.register(InfoContributorAutoConfiguration.class,
				EndpointAutoConfiguration.class);
		this.context.refresh();
		InfoEndpoint endpoint = this.context.getBean(InfoEndpoint.class);
		assertThat(endpoint).isNotNull();
		assertThat(endpoint.invoke().get("git")).isNull();
	}

	@Test
	public void testInfoEndpointOrdering() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context, "info.name:foo");
		this.context.register(CustomInfoContributorsConfig.class,
				ProjectInfoAutoConfiguration.class,
				InfoContributorAutoConfiguration.class, EndpointAutoConfiguration.class);
		this.context.refresh();

		InfoEndpoint endpoint = this.context.getBean(InfoEndpoint.class);
		Map<String, Object> info = endpoint.invoke();
		assertThat(info).isNotNull();
		assertThat(info.get("name")).isEqualTo("foo");
		assertThat(info.get("version")).isEqualTo("1.0");
		Object git = info.get("git");
		assertThat(git).isInstanceOf(Map.class);
	}

	@Test
	public void testFlywayEndpoint() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(EmbeddedDataSourceConfiguration.class,
				FlywayAutoConfiguration.class, EndpointAutoConfiguration.class);
		this.context.refresh();
		FlywayEndpoint endpoint = this.context.getBean(FlywayEndpoint.class);
		assertThat(endpoint).isNotNull();
		assertThat(endpoint.invoke()).hasSize(1);
	}

	@Test
	public void testFlywayEndpointWithMultipleFlywayBeans() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(MultipleFlywayBeansConfig.class,
				FlywayAutoConfiguration.class, EndpointAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBeansOfType(Flyway.class)).hasSize(2);
		assertThat(this.context.getBeansOfType(FlywayEndpoint.class)).hasSize(1);
	}

	@Test
	public void testLiquibaseEndpoint() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(EmbeddedDataSourceConfiguration.class,
				LiquibaseAutoConfiguration.class, EndpointAutoConfiguration.class);
		this.context.refresh();
		LiquibaseEndpoint endpoint = this.context.getBean(LiquibaseEndpoint.class);
		assertThat(endpoint).isNotNull();
		assertThat(endpoint.invoke()).hasSize(1);
	}

	@Test
	public void testLiquibaseEndpointWithMultipleSpringLiquibaseBeans() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(MultipleLiquibaseBeansConfig.class,
				LiquibaseAutoConfiguration.class, EndpointAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBeansOfType(SpringLiquibase.class)).hasSize(2);
		assertThat(this.context.getBeansOfType(LiquibaseEndpoint.class)).hasSize(1);
	}

	private void load(Class<?>... config) {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(config);
		this.context.refresh();
	}

	@Configuration
	static class CustomLoggingConfig {

		@Bean
		LoggingSystem loggingSystem() {
			return LoggingSystem.get(getClass().getClassLoader());
		}

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
	static class CustomInfoContributorsConfig {

		@Bean
		@Order(InfoContributorAutoConfiguration.DEFAULT_ORDER - 1)
		public InfoContributor myInfoContributor() {
			return new InfoContributor() {
				@Override
				public void contribute(Info.Builder builder) {
					builder.withDetail("name", "bar");
					builder.withDetail("version", "1.0");
				}
			};
		}

		@Bean
		@Order(InfoContributorAutoConfiguration.DEFAULT_ORDER + 1)
		public InfoContributor myAnotherContributor(ProjectInfoProperties properties)
				throws IOException, BindException {
			return new GitFullInfoContributor(properties.getGit().getLocation());
		}

		private static class GitFullInfoContributor implements InfoContributor {

			private Map<String, Object> content = new LinkedHashMap<String, Object>();

			GitFullInfoContributor(Resource location) throws BindException, IOException {
				if (location.exists()) {
					Properties gitInfoProperties = PropertiesLoaderUtils
							.loadProperties(location);
					PropertiesPropertySource gitPropertySource = new PropertiesPropertySource(
							"git", gitInfoProperties);
					this.content = new PropertySourcesBinder(gitPropertySource)
							.extractAll("git");
				}
			}

			@Override
			public void contribute(Info.Builder builder) {
				if (!this.content.isEmpty()) {
					builder.withDetail("git", this.content);
				}
			}

		}

	}

	@Configuration
	static class DataSourceConfig {

		@Bean
		public DataSource dataSourceOne() {
			return DataSourceBuilder.create().url("jdbc:hsqldb:mem:changelogdbtest")
					.username("sa").build();
		}

		@Bean
		public DataSource dataSourceTwo() {
			return DataSourceBuilder.create().url("jdbc:hsqldb:mem:changelogdbtest2")
					.username("sa").build();
		}

	}

	@Configuration
	static class MultipleFlywayBeansConfig extends DataSourceConfig {

		@Bean
		public Flyway flywayOne() {
			Flyway flyway = new Flyway();
			flyway.setDataSource(dataSourceOne());
			return flyway;
		}

		@Bean
		public Flyway flywayTwo() {
			Flyway flyway = new Flyway();
			flyway.setDataSource(dataSourceTwo());
			return flyway;
		}

	}

	@Configuration
	static class MultipleLiquibaseBeansConfig extends DataSourceConfig {

		@Bean
		public SpringLiquibase liquibaseOne() {
			SpringLiquibase liquibase = new SpringLiquibase();
			liquibase.setChangeLog("classpath:/db/changelog/db.changelog-master.yaml");
			liquibase.setDataSource(dataSourceOne());
			return liquibase;
		}

		@Bean
		public SpringLiquibase liquibaseTwo() {
			SpringLiquibase liquibase = new SpringLiquibase();
			liquibase.setChangeLog("classpath:/db/changelog/db.changelog-master.yaml");
			liquibase.setDataSource(dataSourceTwo());
			return liquibase;
		}

	}

}
