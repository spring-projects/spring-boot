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

import java.io.File;
import java.util.Arrays;
import java.util.List;

import javax.management.MBeanServer;

import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.netty.client.TcpClientTransport;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
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
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.config.IntegrationManagementConfigurer;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.MessageProcessorMessageSource;
import org.springframework.integration.gateway.RequestReplyExchanger;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.handler.MessageProcessor;
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
		new ApplicationContextRunner(() -> {
			AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
			context.setResourceLoader(
					new FilteringResourceLoader(new DefaultResourceLoader(), "META-INF/spring.integration.properties"));
			return context;
		}).withConfiguration(AutoConfigurations.of(JmxAutoConfiguration.class, IntegrationAutoConfiguration.class))
				.withPropertyValues("spring.integration.channels.auto-create=false",
						"spring.integration.channels.max-unicast-subscribers=2",
						"spring.integration.channels.max-broadcast-subscribers=3",
						"spring.integration.channels.error-require-subscribers=false",
						"spring.integration.channels.error-ignore-failures=false",
						"spring.integration.endpoints.throw-exception-on-late-reply=true",
						"spring.integration.endpoints.read-only-headers=ignoredHeader",
						"spring.integration.endpoints.no-auto-startup=notStartedEndpoint,_org.springframework.integration.errorLogger")
				.withBean("testDirectChannel", DirectChannel.class)
				.withInitializer((applicationContext) -> new IntegrationEnvironmentPostProcessor()
						.postProcessEnvironment(applicationContext.getEnvironment(), null))
				.run((context) -> {
					assertThat(context)
							.getBean(IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME, PublishSubscribeChannel.class)
							.hasFieldOrPropertyWithValue("requireSubscribers", false)
							.hasFieldOrPropertyWithValue("ignoreFailures", false)
							.hasFieldOrPropertyWithValue("maxSubscribers", 3);
					assertThat(context).getBean("testDirectChannel", DirectChannel.class)
							.hasFieldOrPropertyWithValue("maxSubscribers", 2);
					LoggingHandler loggingHandler = context.getBean(LoggingHandler.class);
					assertThat(loggingHandler)
							.hasFieldOrPropertyWithValue("messageBuilderFactory.readOnlyHeaders",
									new String[] { "ignoredHeader" })
							.extracting("integrationProperties", InstanceOfAssertFactories.MAP)
							.containsEntry(
									org.springframework.integration.context.IntegrationProperties.THROW_EXCEPTION_ON_LATE_REPLY,
									"true")
							.containsEntry(
									org.springframework.integration.context.IntegrationProperties.ENDPOINTS_NO_AUTO_STARTUP,
									"notStartedEndpoint,_org.springframework.integration.errorLogger");
					assertThat(context)
							.getBean(IntegrationContextUtils.ERROR_LOGGER_BEAN_NAME, EventDrivenConsumer.class)
							.hasFieldOrPropertyWithValue("autoStartup", false);
				});
	}

	@Test
	void integrationGlobalPropertiesUserBeanOverridesAutoConfiguration() {
		this.contextRunner.withPropertyValues("spring.integration.channels.auto-create=false",
				"spring.integration.channels.max-unicast-subscribers=2",
				"spring.integration.channels.max-broadcast-subscribers=3",
				"spring.integration.channels.error-require-subscribers=false",
				"spring.integration.channels.error-ignore-failures=false",
				"spring.integration.endpoints.throw-exception-on-late-reply=true",
				"spring.integration.endpoints.read-only-headers=ignoredHeader",
				"spring.integration.endpoints.no-auto-startup=notStartedEndpoint,_org.springframework.integration.errorLogger")
				.withBean(IntegrationContextUtils.INTEGRATION_GLOBAL_PROPERTIES_BEAN_NAME,
						org.springframework.integration.context.IntegrationProperties.class, () -> {
							org.springframework.integration.context.IntegrationProperties properties = new org.springframework.integration.context.IntegrationProperties();
							properties.setChannelsMaxUnicastSubscribers(5);
							return properties;
						})
				.withInitializer((applicationContext) -> new IntegrationEnvironmentPostProcessor()
						.postProcessEnvironment(applicationContext.getEnvironment(), null))
				.run((context) -> assertThat(context).getBean(LoggingHandler.class)
						.extracting("integrationProperties", InstanceOfAssertFactories.MAP)
						.containsEntry(
								org.springframework.integration.context.IntegrationProperties.CHANNELS_AUTOCREATE,
								"true")
						.containsEntry(
								org.springframework.integration.context.IntegrationProperties.ERROR_CHANNEL_REQUIRE_SUBSCRIBERS,
								"true")
						.containsEntry(
								org.springframework.integration.context.IntegrationProperties.ERROR_CHANNEL_IGNORE_FAILURES,
								"true")
						.containsEntry(
								org.springframework.integration.context.IntegrationProperties.THROW_EXCEPTION_ON_LATE_REPLY,
								"false")
						.containsEntry(
								org.springframework.integration.context.IntegrationProperties.CHANNELS_MAX_UNICAST_SUBSCRIBERS,
								"5")
						.containsEntry(
								org.springframework.integration.context.IntegrationProperties.CHANNELS_MAX_BROADCAST_SUBSCRIBERS,
								"2147483647")
						.containsEntry(
								org.springframework.integration.context.IntegrationProperties.ENDPOINTS_NO_AUTO_STARTUP,
								"")
						.containsEntry(org.springframework.integration.context.IntegrationProperties.READ_ONLY_HEADERS,
								""));
	}

	@Test
	void integrationGlobalPropertiesFromSpringIntegrationPropertiesFile() {
		// See META-INF/spring.integration.properties
		this.contextRunner
				.withPropertyValues("spring.integration.channels.auto-create=false",
						"spring.integration.endpoints.read-only-headers=ignoredHeader")
				.withInitializer((applicationContext) -> new IntegrationEnvironmentPostProcessor()
						.postProcessEnvironment(applicationContext.getEnvironment(), null))
				.run((context) -> assertThat(context).getBean(LoggingHandler.class)
						.extracting("integrationProperties", InstanceOfAssertFactories.MAP)
						.containsEntry(
								org.springframework.integration.context.IntegrationProperties.CHANNELS_AUTOCREATE,
								"false")
						.containsEntry(org.springframework.integration.context.IntegrationProperties.READ_ONLY_HEADERS,
								"ignoredHeader")
						.containsEntry(
								org.springframework.integration.context.IntegrationProperties.ENDPOINTS_NO_AUTO_STARTUP,
								"testService*"));
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
		MessageSource<?> myMessageSource() {
			return new MessageProcessorMessageSource(mock(MessageProcessor.class));
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class RSocketServerConfiguration {

		@Bean
		IntegrationRSocketEndpoint mockIntegrationRSocketEndpoint() {
			return new IntegrationRSocketEndpoint() {

				@Override
				public Mono<Void> handleMessage(Message<?> message) {
					return null;
				}

				@Override
				public String[] getPath() {
					return new String[] { "/rsocketTestPath" };
				}

			};
		}

	}

	private static final class FilteringResourceLoader implements ResourceLoader {

		private final ResourceLoader delegate;

		private final List<String> resourcesToFilter;

		FilteringResourceLoader(ResourceLoader delegate, String... resourcesToFilter) {
			this.delegate = delegate;
			this.resourcesToFilter = Arrays.asList(resourcesToFilter);
		}

		@Override
		public Resource getResource(String location) {
			if (!this.resourcesToFilter.contains(location)) {
				return this.delegate.getResource(location);
			}
			else {
				return new FileSystemResource(mock(File.class));
			}
		}

		@Override
		public ClassLoader getClassLoader() {
			return this.delegate.getClassLoader();
		}

	}

}
