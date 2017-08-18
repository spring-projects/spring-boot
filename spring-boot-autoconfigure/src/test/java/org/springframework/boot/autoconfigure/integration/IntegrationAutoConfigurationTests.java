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

package org.springframework.boot.autoconfigure.integration;

import java.util.Arrays;
import java.util.List;

import javax.management.MBeanServer;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.autoconfigure.DatabaseInitializationMode;
import org.springframework.boot.autoconfigure.integration.IntegrationAutoConfiguration.IntegrationComponentScanAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.gateway.RequestReplyExchanger;
import org.springframework.integration.support.channel.HeaderChannelRegistry;
import org.springframework.integration.support.management.IntegrationManagementConfigurer;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.test.context.support.TestPropertySourceUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link IntegrationAutoConfiguration}.
 *
 * @author Artem Bilan
 * @author Stephane Nicoll
 * @author Vedran Pavic
 */
public class IntegrationAutoConfigurationTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
			if (this.context.getParent() != null) {
				((ConfigurableApplicationContext) this.context.getParent()).close();
			}
		}
	}

	@Test
	public void integrationIsAvailable() {
		load();
		assertThat(this.context.getBean(TestGateway.class)).isNotNull();
		assertThat(this.context.getBean(IntegrationComponentScanAutoConfiguration.class))
				.isNotNull();
	}

	@Test
	public void explicitIntegrationComponentScan() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(IntegrationComponentScanConfiguration.class,
				JmxAutoConfiguration.class, IntegrationAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(TestGateway.class)).isNotNull();
		assertThat(this.context
				.getBeansOfType(IntegrationComponentScanAutoConfiguration.class))
						.isEmpty();
	}

	@Test
	public void parentContext() {
		load();
		AnnotationConfigApplicationContext parent = this.context;
		this.context = new AnnotationConfigApplicationContext();
		ConfigurationPropertySources.attach(this.context.getEnvironment());
		this.context.setParent(parent);
		this.context.register(JmxAutoConfiguration.class,
				IntegrationAutoConfiguration.class);
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"spring.jmx.default_domain=org.foo");
		this.context.refresh();
		assertThat(this.context.getBean(HeaderChannelRegistry.class)).isNotNull();
	}

	@Test
	public void jmxIntegrationEnabledByDefault() {
		load();
		MBeanServer mBeanServer = this.context.getBean(MBeanServer.class);
		assertDomains(mBeanServer, true, "org.springframework.integration",
				"org.springframework.integration.monitor");
		Object bean = this.context
				.getBean(IntegrationManagementConfigurer.MANAGEMENT_CONFIGURER_NAME);
		assertThat(bean).isNotNull();
	}

	@Test
	public void disableJmxIntegration() {
		load("spring.jmx.enabled=false");
		assertThat(this.context.getBeansOfType(MBeanServer.class)).hasSize(0);
		assertThat(this.context.getBeansOfType(IntegrationManagementConfigurer.class))
				.isEmpty();
	}

	@Test
	public void customizeJmxDomain() {
		load("spring.jmx.default_domain=org.foo");
		MBeanServer mBeanServer = this.context.getBean(MBeanServer.class);
		assertDomains(mBeanServer, true, "org.foo");
		assertDomains(mBeanServer, false, "org.springframework.integration",
				"org.springframework.integration.monitor");
	}

	@Test
	public void primaryExporterIsAllowed() {
		load(new Class[] { CustomMBeanExporter.class });
		assertThat(this.context.getBeansOfType(MBeanExporter.class)).hasSize(2);
		assertThat(this.context.getBean(MBeanExporter.class))
				.isSameAs(this.context.getBean("myMBeanExporter"));
	}

	@Test
	public void integrationJdbcDatabaseInitializerEnabled() {
		load(new Class[] { EmbeddedDataSourceConfiguration.class,
				DataSourceTransactionManagerAutoConfiguration.class,
				JdbcTemplateAutoConfiguration.class, IntegrationAutoConfiguration.class },
				"spring.datasource.generate-unique-name=true",
				"spring.integration.jdbc.initialize-schema=always");
		assertThat(this.context.getBean(IntegrationProperties.class).getJdbc()
				.getInitializeSchema()).isEqualTo(DatabaseInitializationMode.ALWAYS);
		JdbcOperations jdbcOperations = this.context.getBean(JdbcOperations.class);
		assertThat(jdbcOperations.queryForList("select * from INT_MESSAGE")).isEmpty();
		assertThat(jdbcOperations.queryForList("select * from INT_GROUP_TO_MESSAGE"))
				.isEmpty();
		assertThat(jdbcOperations.queryForList("select * from INT_MESSAGE_GROUP"))
				.isEmpty();
		assertThat(jdbcOperations.queryForList("select * from INT_LOCK")).isEmpty();
		assertThat(jdbcOperations.queryForList("select * from INT_CHANNEL_MESSAGE"))
				.isEmpty();
	}

	@Test
	public void integrationJdbcDatabaseInitializerDisabled() {
		load(new Class[] { EmbeddedDataSourceConfiguration.class,
				DataSourceTransactionManagerAutoConfiguration.class,
				JdbcTemplateAutoConfiguration.class, IntegrationAutoConfiguration.class },
				"spring.datasource.generate-unique-name=true",
				"spring.integration.jdbc.initialize-schema=never");
		assertThat(this.context.getBean(IntegrationProperties.class).getJdbc()
				.getInitializeSchema()).isEqualTo(DatabaseInitializationMode.NEVER);
		JdbcOperations jdbcOperations = this.context.getBean(JdbcOperations.class);
		this.thrown.expect(BadSqlGrammarException.class);
		jdbcOperations.queryForList("select * from INT_MESSAGE");
	}

	@Test
	public void integrationJdbcDatabaseInitializerEnabledByDefaultWithEmbeddedDb() {
		load(new Class[] { EmbeddedDataSourceConfiguration.class,
				DataSourceTransactionManagerAutoConfiguration.class,
				JdbcTemplateAutoConfiguration.class, IntegrationAutoConfiguration.class },
				"spring.datasource.generate-unique-name=true");
		assertThat(this.context.getBean(IntegrationProperties.class).getJdbc()
				.getInitializeSchema()).isEqualTo(DatabaseInitializationMode.EMBEDDED);
		JdbcOperations jdbcOperations = this.context.getBean(JdbcOperations.class);
		jdbcOperations.queryForList("select * from INT_MESSAGE").isEmpty();
	}

	private static void assertDomains(MBeanServer mBeanServer, boolean expected,
			String... domains) {
		List<String> actual = Arrays.asList(mBeanServer.getDomains());
		for (String domain : domains) {
			assertThat(actual.contains(domain)).isEqualTo(expected);
		}
	}

	public void load(String... environment) {
		load(null, environment);
	}

	private void load(Class<?>[] configs, String... environment) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		TestPropertyValues.of(environment).applyTo(ctx);
		if (configs != null) {
			ctx.register(configs);
		}
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(ctx, environment);
		ConfigurationPropertySources.attach(ctx.getEnvironment());
		ctx.register(JmxAutoConfiguration.class, IntegrationAutoConfiguration.class);
		ctx.refresh();
		this.context = ctx;
	}

	@Configuration
	static class CustomMBeanExporter {

		@Bean
		@Primary
		public MBeanExporter myMBeanExporter() {
			return mock(MBeanExporter.class);
		}

	}

	@Configuration
	@IntegrationComponentScan
	static class IntegrationComponentScanConfiguration {

	}

	@MessagingGateway
	public interface TestGateway extends RequestReplyExchanger {

	}

}
