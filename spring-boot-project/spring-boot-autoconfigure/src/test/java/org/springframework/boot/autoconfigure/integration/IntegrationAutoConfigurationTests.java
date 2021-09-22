/*
 * Copyright 2012-2021 the original author or authors.
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

import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.netty.client.TcpClientTransport;
import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.integration.IntegrationAutoConfiguration.IntegrationComponentScanConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.autoconfigure.rsocket.RSocketMessagingAutoConfiguration;
import org.springframework.boot.autoconfigure.rsocket.RSocketRequesterAutoConfiguration;
import org.springframework.boot.autoconfigure.rsocket.RSocketServerAutoConfiguration;
import org.springframework.boot.autoconfigure.rsocket.RSocketStrategiesAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.boot.jdbc.DataSourceInitializationMode;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.config.IntegrationManagementConfigurer;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.endpoint.MessageProcessorMessageSource;
import org.springframework.integration.gateway.RequestReplyExchanger;
import org.springframework.integration.rsocket.ClientRSocketConnector;
import org.springframework.integration.rsocket.IntegrationRSocketEndpoint;
import org.springframework.integration.rsocket.ServerRSocketConnector;
import org.springframework.integration.rsocket.ServerRSocketMessageHandler;
import org.springframework.integration.support.channel.HeaderChannelRegistry;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.messaging.Message;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.scheduling.TaskScheduler;

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

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
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
	void whenIntegrationJdbcDataSourceInitializerIsEnabledThenFlywayCanBeUsed() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withConfiguration(AutoConfigurations.of(DataSourceTransactionManagerAutoConfiguration.class,
						JdbcTemplateAutoConfiguration.class, IntegrationAutoConfiguration.class,
						FlywayAutoConfiguration.class))
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
	void rsocketSupportEnabled() {
		this.contextRunner.withUserConfiguration(RSocketServerConfiguration.class)
				.withConfiguration(AutoConfigurations.of(RSocketServerAutoConfiguration.class,
						RSocketStrategiesAutoConfiguration.class, RSocketMessagingAutoConfiguration.class,
						RSocketRequesterAutoConfiguration.class, IntegrationAutoConfiguration.class))
				.withPropertyValues("spring.rsocket.server.port=0", "spring.integration.rsocket.client.port=0",
						"spring.integration.rsocket.client.host=localhost",
						"spring.integration.rsocket.server.message-mapping-enabled=true")
				.run((context) -> {
					assertThat(context).hasSingleBean(ClientRSocketConnector.class).hasBean("clientRSocketConnector")
							.hasSingleBean(ServerRSocketConnector.class)
							.hasSingleBean(ServerRSocketMessageHandler.class)
							.hasSingleBean(RSocketMessageHandler.class);

					ServerRSocketMessageHandler serverRSocketMessageHandler = context
							.getBean(ServerRSocketMessageHandler.class);
					assertThat(context).getBean(RSocketMessageHandler.class).isSameAs(serverRSocketMessageHandler);

					ClientRSocketConnector clientRSocketConnector = context.getBean(ClientRSocketConnector.class);
					ClientTransport clientTransport = (ClientTransport) new DirectFieldAccessor(clientRSocketConnector)
							.getPropertyValue("clientTransport");

					assertThat(clientTransport).isInstanceOf(TcpClientTransport.class);
				});
	}

	@Test
	void taskSchedulerIsNotOverridden() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(TaskSchedulingAutoConfiguration.class))
				.withPropertyValues("spring.task.scheduling.thread-name-prefix=integration-scheduling-",
						"spring.task.scheduling.pool.size=3")
				.run((context) -> {
					assertThat(context).hasSingleBean(TaskScheduler.class);
					assertThat(context).getBean(IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME, TaskScheduler.class)
							.hasFieldOrPropertyWithValue("threadNamePrefix", "integration-scheduling-")
							.hasFieldOrPropertyWithValue("scheduledExecutor.corePoolSize", 3);
				});
	}

	@Test
	void taskSchedulerCanBeCustomized() {
		TaskScheduler customTaskScheduler = mock(TaskScheduler.class);
		this.contextRunner.withConfiguration(AutoConfigurations.of(TaskSchedulingAutoConfiguration.class))
				.withBean(IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME, TaskScheduler.class,
						() -> customTaskScheduler)
				.run((context) -> {
					assertThat(context).hasSingleBean(TaskScheduler.class);
					assertThat(context).getBean(IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME)
							.isSameAs(customTaskScheduler);
				});
	}

	@Test
	void integrationGlobalPropertiesAutoConfigured() {
		this.contextRunner.withPropertyValues("spring.integration.channel.auto-create=false",
				"spring.integration.channel.max-unicast-subscribers=2",
				"spring.integration.channel.max-broadcast-subscribers=3",
				"spring.integration.error.require-subscribers=false", "spring.integration.error.ignore-failures=false",
				"spring.integration.endpoint.throw-exception-on-late-reply=true",
				"spring.integration.endpoint.read-only-headers=ignoredHeader",
				"spring.integration.endpoint.no-auto-startup=notStartedEndpoint,_org.springframework.integration.errorLogger")
				.run((context) -> {
					assertThat(context)
							.hasSingleBean(org.springframework.integration.context.IntegrationProperties.class);
					org.springframework.integration.context.IntegrationProperties integrationProperties = context
							.getBean(org.springframework.integration.context.IntegrationProperties.class);
					assertThat(integrationProperties.isChannelsAutoCreate()).isFalse();
					assertThat(integrationProperties.getChannelsMaxUnicastSubscribers()).isEqualTo(2);
					assertThat(integrationProperties.getChannelsMaxBroadcastSubscribers()).isEqualTo(3);
					assertThat(integrationProperties.isErrorChannelRequireSubscribers()).isFalse();
					assertThat(integrationProperties.isErrorChannelIgnoreFailures()).isFalse();
					assertThat(integrationProperties.isMessagingTemplateThrowExceptionOnLateReply()).isTrue();
					assertThat(integrationProperties.getReadOnlyHeaders()).containsOnly("ignoredHeader");
					assertThat(integrationProperties.getNoAutoStartupEndpoints()).containsOnly("notStartedEndpoint",
							"_org.springframework.integration.errorLogger");
				});
	}

	@Test
	void integrationGlobalPropertiesUseConsistentDefault() {
		org.springframework.integration.context.IntegrationProperties defaultIntegrationProperties = new org.springframework.integration.context.IntegrationProperties();
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(org.springframework.integration.context.IntegrationProperties.class);
			org.springframework.integration.context.IntegrationProperties integrationProperties = context
					.getBean(org.springframework.integration.context.IntegrationProperties.class);
			assertThat(integrationProperties.isChannelsAutoCreate())
					.isEqualTo(defaultIntegrationProperties.isChannelsAutoCreate());
			assertThat(integrationProperties.getChannelsMaxUnicastSubscribers())
					.isEqualTo(defaultIntegrationProperties.getChannelsMaxBroadcastSubscribers());
			assertThat(integrationProperties.getChannelsMaxBroadcastSubscribers())
					.isEqualTo(defaultIntegrationProperties.getChannelsMaxBroadcastSubscribers());
			assertThat(integrationProperties.isErrorChannelRequireSubscribers())
					.isEqualTo(defaultIntegrationProperties.isErrorChannelIgnoreFailures());
			assertThat(integrationProperties.isErrorChannelIgnoreFailures())
					.isEqualTo(defaultIntegrationProperties.isErrorChannelIgnoreFailures());
			assertThat(integrationProperties.isMessagingTemplateThrowExceptionOnLateReply())
					.isEqualTo(defaultIntegrationProperties.isMessagingTemplateThrowExceptionOnLateReply());
			assertThat(integrationProperties.getReadOnlyHeaders())
					.isEqualTo(defaultIntegrationProperties.getReadOnlyHeaders());
			assertThat(integrationProperties.getNoAutoStartupEndpoints())
					.isEqualTo(defaultIntegrationProperties.getNoAutoStartupEndpoints());
		});
	}

	@Test
	void integrationGlobalPropertiesUserBeanOverridesAutoConfiguration() {
		org.springframework.integration.context.IntegrationProperties userIntegrationProperties = new org.springframework.integration.context.IntegrationProperties();
		this.contextRunner.withPropertyValues()
				.withBean(IntegrationContextUtils.INTEGRATION_GLOBAL_PROPERTIES_BEAN_NAME,
						org.springframework.integration.context.IntegrationProperties.class,
						() -> userIntegrationProperties)
				.run((context) -> {
					assertThat(context)
							.hasSingleBean(org.springframework.integration.context.IntegrationProperties.class);
					assertThat(context.getBean(org.springframework.integration.context.IntegrationProperties.class))
							.isSameAs(userIntegrationProperties);
				});
	}

	@Test
	void integrationGlobalPropertiesFromSpringIntegrationPropertiesFile() {
		this.contextRunner
				.withPropertyValues("spring.integration.channel.auto-create=false",
						"spring.integration.endpoint.read-only-headers=ignoredHeader")
				.withInitializer((applicationContext) -> new IntegrationPropertiesEnvironmentPostProcessor()
						.postProcessEnvironment(applicationContext.getEnvironment(), null))
				.run((context) -> {
					assertThat(context)
							.hasSingleBean(org.springframework.integration.context.IntegrationProperties.class);
					org.springframework.integration.context.IntegrationProperties integrationProperties = context
							.getBean(org.springframework.integration.context.IntegrationProperties.class);
					assertThat(integrationProperties.isChannelsAutoCreate()).isFalse();
					assertThat(integrationProperties.getReadOnlyHeaders()).containsOnly("ignoredHeader");
					// See META-INF/spring.integration.properties
					assertThat(integrationProperties.getNoAutoStartupEndpoints()).containsOnly("testService*");
				});
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomMBeanExporter {

		@Bean
		@Primary
		MBeanExporter myMBeanExporter() {
			return mock(MBeanExporter.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@IntegrationComponentScan
	static class CustomIntegrationComponentScanConfiguration {

	}

	@MessagingGateway
	interface TestGateway extends RequestReplyExchanger {

	}

	@Configuration(proxyBeanMethods = false)
	static class MessageSourceConfiguration {

		@Bean
		org.springframework.integration.core.MessageSource<?> myMessageSource() {
			return new MessageProcessorMessageSource(
					mock(org.springframework.integration.handler.MessageProcessor.class));
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class RSocketServerConfiguration {

		@Bean
		IntegrationRSocketEndpoint mockIntegrationRSocketEndpoint() {
			return new IntegrationRSocketEndpoint() {

				@Override
				public reactor.core.publisher.Mono<Void> handleMessage(Message<?> message) {
					return null;
				}

				@Override
				public String[] getPath() {
					return new String[] { "/rsocketTestPath" };
				}

			};
		}

	}

}
