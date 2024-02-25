/*
 * Copyright 2012-2023 the original author or authors.
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

import java.time.Duration;

import javax.management.MBeanServer;
import javax.sql.DataSource;

import io.rsocket.transport.netty.server.TcpServerTransport;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxProperties;
import org.springframework.boot.autoconfigure.rsocket.RSocketMessagingAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.OnDatabaseInitializationCondition;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.context.properties.source.MutuallyExclusiveConfigurationPropertiesException;
import org.springframework.boot.task.TaskSchedulerBuilder;
import org.springframework.boot.task.ThreadPoolTaskSchedulerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.config.EnableIntegrationManagement;
import org.springframework.integration.config.IntegrationComponentScanRegistrar;
import org.springframework.integration.config.IntegrationManagementConfigurer;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.jdbc.store.JdbcMessageStore;
import org.springframework.integration.jmx.config.EnableIntegrationMBeanExport;
import org.springframework.integration.monitor.IntegrationMBeanExporter;
import org.springframework.integration.rsocket.ClientRSocketConnector;
import org.springframework.integration.rsocket.IntegrationRSocketEndpoint;
import org.springframework.integration.rsocket.ServerRSocketConnector;
import org.springframework.integration.rsocket.ServerRSocketMessageHandler;
import org.springframework.integration.rsocket.outbound.RSocketOutboundGateway;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.util.StringUtils;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} for Spring Integration.
 *
 * @author Artem Bilan
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Vedran Pavic
 * @author Madhura Bhave
 * @since 1.1.0
 */
@AutoConfiguration(after = { DataSourceAutoConfiguration.class, JmxAutoConfiguration.class,
		TaskSchedulingAutoConfiguration.class })
@ConditionalOnClass(EnableIntegration.class)
@EnableConfigurationProperties({ IntegrationProperties.class, JmxProperties.class })
public class IntegrationAutoConfiguration {

	/**
	 * Creates an instance of
	 * {@link org.springframework.integration.context.IntegrationProperties} bean if there
	 * is no existing bean with the name
	 * {@link IntegrationContextUtils.INTEGRATION_GLOBAL_PROPERTIES_BEAN_NAME}. The
	 * properties are mapped from the provided {@link IntegrationProperties} object.
	 * @param properties the {@link IntegrationProperties} object containing the
	 * properties to be mapped
	 * @return the created
	 * {@link org.springframework.integration.context.IntegrationProperties} bean
	 */
	@Bean(name = IntegrationContextUtils.INTEGRATION_GLOBAL_PROPERTIES_BEAN_NAME)
	@ConditionalOnMissingBean(name = IntegrationContextUtils.INTEGRATION_GLOBAL_PROPERTIES_BEAN_NAME)
	public static org.springframework.integration.context.IntegrationProperties integrationGlobalProperties(
			IntegrationProperties properties) {
		org.springframework.integration.context.IntegrationProperties integrationProperties = new org.springframework.integration.context.IntegrationProperties();
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(properties.getChannel().isAutoCreate()).to(integrationProperties::setChannelsAutoCreate);
		map.from(properties.getChannel().getMaxUnicastSubscribers())
			.to(integrationProperties::setChannelsMaxUnicastSubscribers);
		map.from(properties.getChannel().getMaxBroadcastSubscribers())
			.to(integrationProperties::setChannelsMaxBroadcastSubscribers);
		map.from(properties.getError().isRequireSubscribers())
			.to(integrationProperties::setErrorChannelRequireSubscribers);
		map.from(properties.getError().isIgnoreFailures()).to(integrationProperties::setErrorChannelIgnoreFailures);
		map.from(properties.getEndpoint().isThrowExceptionOnLateReply())
			.to(integrationProperties::setMessagingTemplateThrowExceptionOnLateReply);
		map.from(properties.getEndpoint().getReadOnlyHeaders())
			.as(StringUtils::toStringArray)
			.to(integrationProperties::setReadOnlyHeaders);
		map.from(properties.getEndpoint().getNoAutoStartup())
			.as(StringUtils::toStringArray)
			.to(integrationProperties::setNoAutoStartupEndpoints);
		return integrationProperties;
	}

	/**
	 * Basic Spring Integration configuration.
	 */
	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	protected static class IntegrationConfiguration {

		/**
		 * Creates a default PollerMetadata bean if no other bean with the same name is
		 * present. The default PollerMetadata is configured based on the
		 * integrationProperties.
		 * @param integrationProperties the IntegrationProperties object containing the
		 * poller configuration
		 * @return the PollerMetadata bean
		 * @throws MutuallyExclusiveConfigurationPropertiesException if multiple non-null
		 * values are found in the poller configuration
		 */
		@Bean(PollerMetadata.DEFAULT_POLLER)
		@ConditionalOnMissingBean(name = PollerMetadata.DEFAULT_POLLER)
		public PollerMetadata defaultPollerMetadata(IntegrationProperties integrationProperties) {
			IntegrationProperties.Poller poller = integrationProperties.getPoller();
			MutuallyExclusiveConfigurationPropertiesException.throwIfMultipleNonNullValuesIn((entries) -> {
				entries.put("spring.integration.poller.cron",
						StringUtils.hasText(poller.getCron()) ? poller.getCron() : null);
				entries.put("spring.integration.poller.fixed-delay", poller.getFixedDelay());
				entries.put("spring.integration.poller.fixed-rate", poller.getFixedRate());
			});
			PollerMetadata pollerMetadata = new PollerMetadata();
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(poller::getMaxMessagesPerPoll).to(pollerMetadata::setMaxMessagesPerPoll);
			map.from(poller::getReceiveTimeout).as(Duration::toMillis).to(pollerMetadata::setReceiveTimeout);
			map.from(poller).as(this::asTrigger).to(pollerMetadata::setTrigger);
			return pollerMetadata;
		}

		/**
		 * Converts an IntegrationProperties.Poller object into a Trigger object.
		 * @param poller the IntegrationProperties.Poller object to convert
		 * @return a Trigger object based on the provided poller object, or null if the
		 * poller object is null
		 */
		private Trigger asTrigger(IntegrationProperties.Poller poller) {
			if (StringUtils.hasText(poller.getCron())) {
				return new CronTrigger(poller.getCron());
			}
			if (poller.getFixedDelay() != null) {
				return createPeriodicTrigger(poller.getFixedDelay(), poller.getInitialDelay(), false);
			}
			if (poller.getFixedRate() != null) {
				return createPeriodicTrigger(poller.getFixedRate(), poller.getInitialDelay(), true);
			}
			return null;
		}

		/**
		 * Creates a periodic trigger with the specified period, initial delay, and fixed
		 * rate.
		 * @param period the duration between each execution of the trigger
		 * @param initialDelay the duration to wait before the first execution of the
		 * trigger
		 * @param fixedRate true if the trigger should be executed at a fixed rate, false
		 * if it should be executed at a fixed delay
		 * @return the created periodic trigger
		 */
		private Trigger createPeriodicTrigger(Duration period, Duration initialDelay, boolean fixedRate) {
			PeriodicTrigger trigger = new PeriodicTrigger(period);
			if (initialDelay != null) {
				trigger.setInitialDelay(initialDelay);
			}
			trigger.setFixedRate(fixedRate);
			return trigger;
		}

	}

	/**
	 * Expose a standard {@link ThreadPoolTaskScheduler} if the user has not enabled task
	 * scheduling explicitly.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(TaskSchedulerBuilder.class)
	@ConditionalOnMissingBean(name = IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME)
	@SuppressWarnings("removal")
	protected static class IntegrationTaskSchedulerConfiguration {

		/**
		 * Returns the task scheduler bean for the integration context.
		 * @param taskSchedulerBuilder The task scheduler builder.
		 * @param threadPoolTaskSchedulerBuilderProvider The provider for the thread pool
		 * task scheduler builder.
		 * @return The task scheduler bean.
		 */
		@Bean(name = IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME)
		public ThreadPoolTaskScheduler taskScheduler(TaskSchedulerBuilder taskSchedulerBuilder,
				ObjectProvider<ThreadPoolTaskSchedulerBuilder> threadPoolTaskSchedulerBuilderProvider) {
			ThreadPoolTaskSchedulerBuilder threadPoolTaskSchedulerBuilder = threadPoolTaskSchedulerBuilderProvider
				.getIfUnique();
			if (threadPoolTaskSchedulerBuilder != null) {
				return threadPoolTaskSchedulerBuilder.build();
			}
			return taskSchedulerBuilder.build();
		}

	}

	/**
	 * Spring Integration JMX configuration.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(EnableIntegrationMBeanExport.class)
	@ConditionalOnMissingBean(value = IntegrationMBeanExporter.class, search = SearchStrategy.CURRENT)
	@ConditionalOnBean(MBeanServer.class)
	@ConditionalOnProperty(prefix = "spring.jmx", name = "enabled", havingValue = "true", matchIfMissing = true)
	protected static class IntegrationJmxConfiguration {

		/**
		 * Creates and configures an instance of IntegrationMBeanExporter.
		 * @param beanFactory the BeanFactory used to retrieve the MBeanServer bean
		 * @param properties the JmxProperties used to configure the exporter
		 * @return the configured IntegrationMBeanExporter instance
		 */
		@Bean
		public IntegrationMBeanExporter integrationMbeanExporter(BeanFactory beanFactory, JmxProperties properties) {
			IntegrationMBeanExporter exporter = new IntegrationMBeanExporter();
			String defaultDomain = properties.getDefaultDomain();
			if (StringUtils.hasLength(defaultDomain)) {
				exporter.setDefaultDomain(defaultDomain);
			}
			exporter.setServer(beanFactory.getBean(properties.getServer(), MBeanServer.class));
			return exporter;
		}

	}

	/**
	 * Integration management configuration.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(EnableIntegrationManagement.class)
	@ConditionalOnMissingBean(value = IntegrationManagementConfigurer.class,
			name = IntegrationManagementConfigurer.MANAGEMENT_CONFIGURER_NAME, search = SearchStrategy.CURRENT)
	protected static class IntegrationManagementConfiguration {

		/**
		 * EnableIntegrationManagementConfiguration class.
		 */
		@Configuration(proxyBeanMethods = false)
		@EnableIntegrationManagement(
				defaultLoggingEnabled = "${spring.integration.management.default-logging-enabled:true}",
				observationPatterns = "${spring.integration.management.observation-patterns:}")
		protected static class EnableIntegrationManagementConfiguration {

		}

	}

	/**
	 * Integration component scan configuration.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(IntegrationComponentScanRegistrar.class)
	@Import(IntegrationAutoConfigurationScanRegistrar.class)
	protected static class IntegrationComponentScanConfiguration {

	}

	/**
	 * Integration JDBC configuration.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(JdbcMessageStore.class)
	@ConditionalOnSingleCandidate(DataSource.class)
	@Conditional(OnIntegrationDatasourceInitializationCondition.class)
	protected static class IntegrationJdbcConfiguration {

		/**
		 * Creates a new instance of IntegrationDataSourceScriptDatabaseInitializer if
		 * there is no existing bean of type
		 * IntegrationDataSourceScriptDatabaseInitializer. This initializer is responsible
		 * for initializing the integration data source by executing SQL scripts on the
		 * provided data source.
		 * @param dataSource the data source to be initialized
		 * @param properties the integration properties containing JDBC configuration
		 * @return a new instance of IntegrationDataSourceScriptDatabaseInitializer
		 */
		@Bean
		@ConditionalOnMissingBean(IntegrationDataSourceScriptDatabaseInitializer.class)
		public IntegrationDataSourceScriptDatabaseInitializer integrationDataSourceInitializer(DataSource dataSource,
				IntegrationProperties properties) {
			return new IntegrationDataSourceScriptDatabaseInitializer(dataSource, properties.getJdbc());
		}

	}

	/**
	 * Integration RSocket configuration.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ IntegrationRSocketEndpoint.class, RSocketRequester.class, io.rsocket.RSocket.class })
	@Conditional(IntegrationRSocketConfiguration.AnyRSocketChannelAdapterAvailable.class)
	protected static class IntegrationRSocketConfiguration {

		/**
		 * Check if either an {@link IntegrationRSocketEndpoint} or
		 * {@link RSocketOutboundGateway} bean is available.
		 */
		static class AnyRSocketChannelAdapterAvailable extends AnyNestedCondition {

			/**
			 * Checks if there is any RSocket channel adapter available.
			 * @return true if there is any RSocket channel adapter available, false
			 * otherwise
			 */
			AnyRSocketChannelAdapterAvailable() {
				super(ConfigurationPhase.REGISTER_BEAN);
			}

			/**
			 * IntegrationRSocketEndpointAvailable class.
			 */
			@ConditionalOnBean(IntegrationRSocketEndpoint.class)
			static class IntegrationRSocketEndpointAvailable {

			}

			/**
			 * RSocketOutboundGatewayAvailable class.
			 */
			@ConditionalOnBean(RSocketOutboundGateway.class)
			static class RSocketOutboundGatewayAvailable {

			}

		}

		/**
		 * IntegrationRSocketServerConfiguration class.
		 */
		@Configuration(proxyBeanMethods = false)
		@ConditionalOnClass(TcpServerTransport.class)
		@AutoConfigureBefore(RSocketMessagingAutoConfiguration.class)
		protected static class IntegrationRSocketServerConfiguration {

			/**
			 * Creates a new instance of {@link RSocketMessageHandler} if no bean of type
			 * {@link ServerRSocketMessageHandler} is present.
			 * @param rSocketStrategies the {@link RSocketStrategies} to be used by the
			 * message handler
			 * @param integrationProperties the {@link IntegrationProperties} containing
			 * the RSocket server configuration
			 * @return the created {@link RSocketMessageHandler}
			 */
			@Bean
			@ConditionalOnMissingBean(ServerRSocketMessageHandler.class)
			public RSocketMessageHandler serverRSocketMessageHandler(RSocketStrategies rSocketStrategies,
					IntegrationProperties integrationProperties) {

				RSocketMessageHandler messageHandler = new ServerRSocketMessageHandler(
						integrationProperties.getRsocket().getServer().isMessageMappingEnabled());
				messageHandler.setRSocketStrategies(rSocketStrategies);
				return messageHandler;
			}

			/**
			 * Creates a new instance of {@link ServerRSocketConnector} if no other bean
			 * of the same type is present.
			 * @param messageHandler the {@link ServerRSocketMessageHandler} to be used by
			 * the connector
			 * @return a new instance of {@link ServerRSocketConnector}
			 */
			@Bean
			@ConditionalOnMissingBean
			public ServerRSocketConnector serverRSocketConnector(ServerRSocketMessageHandler messageHandler) {
				return new ServerRSocketConnector(messageHandler);
			}

		}

		/**
		 * IntegrationRSocketClientConfiguration class.
		 */
		@Configuration(proxyBeanMethods = false)
		protected static class IntegrationRSocketClientConfiguration {

			/**
			 * Creates a ClientRSocketConnector bean for connecting to a remote RSocket
			 * server. This bean is conditionally created only if no other bean of the
			 * same type is present, and if the RemoteRSocketServerAddressConfigured
			 * condition is met.
			 * @param integrationProperties The integration properties containing the
			 * RSocket client configuration.
			 * @param rSocketStrategies The RSocket strategies to be used by the client.
			 * @return The created ClientRSocketConnector bean.
			 */
			@Bean
			@ConditionalOnMissingBean
			@Conditional(RemoteRSocketServerAddressConfigured.class)
			public ClientRSocketConnector clientRSocketConnector(IntegrationProperties integrationProperties,
					RSocketStrategies rSocketStrategies) {

				IntegrationProperties.RSocket.Client client = integrationProperties.getRsocket().getClient();
				ClientRSocketConnector clientRSocketConnector = (client.getUri() != null)
						? new ClientRSocketConnector(client.getUri())
						: new ClientRSocketConnector(client.getHost(), client.getPort());
				clientRSocketConnector.setRSocketStrategies(rSocketStrategies);
				return clientRSocketConnector;
			}

			/**
			 * Check if a remote address is configured for the RSocket Integration client.
			 */
			static class RemoteRSocketServerAddressConfigured extends AnyNestedCondition {

				/**
				 * Constructor for the RemoteRSocketServerAddressConfigured class.
				 *
				 * Initializes a new instance of the class with the specified
				 * configuration phase.
				 * @param configurationPhase The configuration phase for the
				 * RemoteRSocketServerAddressConfigured class.
				 */
				RemoteRSocketServerAddressConfigured() {
					super(ConfigurationPhase.REGISTER_BEAN);
				}

				/**
				 * WebSocketAddressConfigured class.
				 */
				@ConditionalOnProperty(prefix = "spring.integration.rsocket.client", name = "uri")
				static class WebSocketAddressConfigured {

				}

				/**
				 * TcpAddressConfigured class.
				 */
				@ConditionalOnProperty(prefix = "spring.integration.rsocket.client", name = { "host", "port" })
				static class TcpAddressConfigured {

				}

			}

		}

	}

	/**
	 * OnIntegrationDatasourceInitializationCondition class.
	 */
	static class OnIntegrationDatasourceInitializationCondition extends OnDatabaseInitializationCondition {

		/**
		 * Constructor for the OnIntegrationDatasourceInitializationCondition class.
		 * Initializes the condition with the specified name and property key.
		 * @param name the name of the condition
		 * @param propertyKey the property key for the condition
		 */
		OnIntegrationDatasourceInitializationCondition() {
			super("Integration", "spring.integration.jdbc.initialize-schema");
		}

	}

}
