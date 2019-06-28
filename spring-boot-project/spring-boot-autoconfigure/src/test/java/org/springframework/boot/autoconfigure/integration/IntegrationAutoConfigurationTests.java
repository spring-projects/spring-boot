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

package org.springframework.boot.autoconfigure.integration;

import javax.management.MBeanServer;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.integration.IntegrationAutoConfiguration.IntegrationComponentScanConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.jdbc.DataSourceInitializationMode;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.config.IntegrationManagementConfigurer;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.endpoint.MessageProcessorMessageSource;
import org.springframework.integration.gateway.RequestReplyExchanger;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.support.channel.HeaderChannelRegistry;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jmx.export.MBeanExporter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link IntegrationAutoConfiguration}.
 *
 * @author Artem Bilan
 * @author Stephane Nicoll
 * @author Vedran Pavic
 */
class IntegrationAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(JmxAutoConfiguration.class, IntegrationAutoConfiguration.class));

	@Test
	void integrationIsAvailable() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(TestGateway.class);
			assertThat(context).hasSingleBean(IntegrationComponentScanConfiguration.class);
		});
	}

	@Test
	void explicitIntegrationComponentScan() {
		this.contextRunner.withUserConfiguration(CustomIntegrationComponentScanConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(TestGateway.class);
			assertThat(context).doesNotHaveBean(IntegrationComponentScanConfiguration.class);
		});
	}

	@Test
	void noMBeanServerAvailable() {
		ApplicationContextRunner contextRunnerWithoutJmx = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(IntegrationAutoConfiguration.class));
		contextRunnerWithoutJmx.run((context) -> {
			assertThat(context).hasSingleBean(TestGateway.class);
			assertThat(context).hasSingleBean(IntegrationComponentScanConfiguration.class);
		});
	}

	@Test
	void parentContext() {
		this.contextRunner.run((context) -> this.contextRunner.withParent(context)
				.withPropertyValues("spring.jmx.default_domain=org.foo")
				.run((child) -> assertThat(child).hasSingleBean(HeaderChannelRegistry.class)));
	}

	@Test
	void enableJmxIntegration() {
		this.contextRunner.withPropertyValues("spring.jmx.enabled=true").run((context) -> {
			MBeanServer mBeanServer = context.getBean(MBeanServer.class);
			assertThat(mBeanServer.getDomains()).contains("org.springframework.integration",
					"org.springframework.integration.monitor");
			assertThat(context).hasBean(IntegrationManagementConfigurer.MANAGEMENT_CONFIGURER_NAME);
		});
	}

	@Test
	void jmxIntegrationIsDisabledByDefault() {
		this.contextRunner.run((context) -> {
			assertThat(context).doesNotHaveBean(MBeanServer.class);
			assertThat(context).hasSingleBean(IntegrationManagementConfigurer.class);
		});
	}

	@Test
	void customizeJmxDomain() {
		this.contextRunner.withPropertyValues("spring.jmx.enabled=true", "spring.jmx.default_domain=org.foo")
				.run((context) -> {
					MBeanServer mBeanServer = context.getBean(MBeanServer.class);
					assertThat(mBeanServer.getDomains()).contains("org.foo").doesNotContain(
							"org.springframework.integration", "org.springframework.integration.monitor");
				});
	}

	@Test
	void primaryExporterIsAllowed() {
		this.contextRunner.withPropertyValues("spring.jmx.enabled=true")
				.withUserConfiguration(CustomMBeanExporter.class).run((context) -> {
					assertThat(context).getBeans(MBeanExporter.class).hasSize(2);
					assertThat(context.getBean(MBeanExporter.class)).isSameAs(context.getBean("myMBeanExporter"));
				});
	}

	@Test
	void integrationJdbcDataSourceInitializerEnabled() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withConfiguration(AutoConfigurations.of(DataSourceTransactionManagerAutoConfiguration.class,
						JdbcTemplateAutoConfiguration.class, IntegrationAutoConfiguration.class))
				.withPropertyValues("spring.datasource.generate-unique-name=true",
						"spring.integration.jdbc.initialize-schema=always")
				.run((context) -> {
					IntegrationProperties properties = context.getBean(IntegrationProperties.class);
					assertThat(properties.getJdbc().getInitializeSchema())
							.isEqualTo(DataSourceInitializationMode.ALWAYS);
					JdbcOperations jdbc = context.getBean(JdbcOperations.class);
					assertThat(jdbc.queryForList("select * from INT_MESSAGE")).isEmpty();
					assertThat(jdbc.queryForList("select * from INT_GROUP_TO_MESSAGE")).isEmpty();
					assertThat(jdbc.queryForList("select * from INT_MESSAGE_GROUP")).isEmpty();
					assertThat(jdbc.queryForList("select * from INT_LOCK")).isEmpty();
					assertThat(jdbc.queryForList("select * from INT_CHANNEL_MESSAGE")).isEmpty();
				});
	}

	@Test
	void integrationJdbcDataSourceInitializerDisabled() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withConfiguration(AutoConfigurations.of(DataSourceTransactionManagerAutoConfiguration.class,
						JdbcTemplateAutoConfiguration.class, IntegrationAutoConfiguration.class))
				.withPropertyValues("spring.datasource.generate-unique-name=true",
						"spring.integration.jdbc.initialize-schema=never")
				.run((context) -> {
					IntegrationProperties properties = context.getBean(IntegrationProperties.class);
					assertThat(properties.getJdbc().getInitializeSchema())
							.isEqualTo(DataSourceInitializationMode.NEVER);
					JdbcOperations jdbc = context.getBean(JdbcOperations.class);
					assertThatExceptionOfType(BadSqlGrammarException.class)
							.isThrownBy(() -> jdbc.queryForList("select * from INT_MESSAGE"));
				});
	}

	@Test
	void integrationJdbcDataSourceInitializerEnabledByDefaultWithEmbeddedDb() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withConfiguration(AutoConfigurations.of(DataSourceTransactionManagerAutoConfiguration.class,
						JdbcTemplateAutoConfiguration.class, IntegrationAutoConfiguration.class))
				.withPropertyValues("spring.datasource.generate-unique-name=true").run((context) -> {
					IntegrationProperties properties = context.getBean(IntegrationProperties.class);
					assertThat(properties.getJdbc().getInitializeSchema())
							.isEqualTo(DataSourceInitializationMode.EMBEDDED);
					JdbcOperations jdbc = context.getBean(JdbcOperations.class);
					assertThat(jdbc.queryForList("select * from INT_MESSAGE")).isEmpty();
				});
	}

	@Test
	void integrationEnablesDefaultCounts() {
		this.contextRunner.withUserConfiguration(MessageSourceConfiguration.class).run((context) -> {
			assertThat(context).hasBean("myMessageSource");
			assertThat(((MessageProcessorMessageSource) context.getBean("myMessageSource")).isCountsEnabled()).isTrue();
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomMBeanExporter {

		@Bean
		@Primary
		public MBeanExporter myMBeanExporter() {
			return mock(MBeanExporter.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@IntegrationComponentScan
	static class CustomIntegrationComponentScanConfiguration {

	}

	@MessagingGateway
	public interface TestGateway extends RequestReplyExchanger {

	}

	@Configuration(proxyBeanMethods = false)
	static class MessageSourceConfiguration {

		@Bean
		public MessageSource<?> myMessageSource() {
			return new MessageProcessorMessageSource(mock(MessageProcessor.class));
		}

	}

}
