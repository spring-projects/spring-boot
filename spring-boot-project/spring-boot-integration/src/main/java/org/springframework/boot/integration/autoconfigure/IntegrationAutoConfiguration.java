/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.integration.autoconfigure;

import java.time.Duration;

import javax.management.MBeanServer;
import javax.sql.DataSource;

import io.rsocket.transport.netty.server.TcpServerTransport;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnThreading;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxProperties;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.boot.autoconfigure.thread.Threading;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.context.properties.source.MutuallyExclusiveConfigurationPropertiesException;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.init.DataSourceScriptDatabaseInitializer;
import org.springframework.boot.sql.autoconfigure.init.OnDatabaseInitializationCondition;
import org.springframework.boot.task.SimpleAsyncTaskSchedulerBuilder;
import org.springframework.boot.task.ThreadPoolTaskSchedulerBuilder;
import org.springframework.context.ApplicationContext;
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
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;
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
 * @author Yong-Hyun Kim
 * @author Yanming Zhou
 * @since 4.0.0
 */
@AutoConfiguration(beforeName = "org.springframework.boot.rsocket.autoconfigure.RSocketMessagingAutoConfiguration",
		after = { DataSourceAutoConfiguration.class, JmxAutoConfiguration.class,
				TaskSchedulingAutoConfiguration.class })
@ConditionalOnClass(EnableIntegration.class)
@EnableConfigurationProperties({ IntegrationProperties.class, JmxProperties.class })
public class IntegrationAutoConfiguration {

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
		map.from(properties.getEndpoint().getDefaultTimeout())
			.as(Duration::toMillis)
			.to(integrationProperties::setEndpointsDefaultTimeout);
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

		@Bean(PollerMetadata.DEFAULT_POLLER)
		@ConditionalOnMissingBean(name = PollerMetadata.DEFAULT_POLLER)
		public PollerMetadata defaultPollerMetadata(IntegrationProperties integrationProperties,
				ObjectProvider<PollerMetadataCustomizer> customizers) {
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
			customizers.orderedStream().forEach((customizer) -> customizer.customize(pollerMetadata));
			return pollerMetadata;
		}

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
	 * Expose a standard {@link org.springframework.scheduling.TaskScheduler
	 * TaskScheduler} if the user has not enabled task scheduling explicitly. A
	 * {@link SimpleAsyncTaskScheduler} is exposed if the user enables virtual threads via
	 * {@code spring.threads.virtual.enabled=true}, otherwise
	 * {@link ThreadPoolTaskScheduler}.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(name = IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME)
	protected static class IntegrationTaskSchedulerConfiguration {

		@Bean(name = IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME)
		@ConditionalOnBean(ThreadPoolTaskSchedulerBuilder.class)
		@ConditionalOnThreading(Threading.PLATFORM)
		public ThreadPoolTaskScheduler taskScheduler(ThreadPoolTaskSchedulerBuilder threadPoolTaskSchedulerBuilder) {
			return threadPoolTaskSchedulerBuilder.build();
		}

		@Bean(name = IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME)
		@ConditionalOnBean(SimpleAsyncTaskSchedulerBuilder.class)
		@ConditionalOnThreading(Threading.VIRTUAL)
		public SimpleAsyncTaskScheduler taskSchedulerVirtualThreads(
				SimpleAsyncTaskSchedulerBuilder simpleAsyncTaskSchedulerBuilder) {
			return simpleAsyncTaskSchedulerBuilder.build();
		}

	}

	/**
	 * Spring Integration JMX configuration.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(EnableIntegrationMBeanExport.class)
	@ConditionalOnMissingBean(value = IntegrationMBeanExporter.class, search = SearchStrategy.CURRENT)
	@ConditionalOnBean(MBeanServer.class)
	@ConditionalOnBooleanProperty("spring.jmx.enabled")
	protected static class IntegrationJmxConfiguration {

		@Bean
		public static IntegrationMBeanExporter integrationMbeanExporter(ApplicationContext applicationContext) {
			return new IntegrationMBeanExporter() {

				@Override
				public void afterSingletonsInstantiated() {
					JmxProperties properties = applicationContext.getBean(JmxProperties.class);
					String defaultDomain = properties.getDefaultDomain();
					if (StringUtils.hasLength(defaultDomain)) {
						setDefaultDomain(defaultDomain);
					}
					setServer(applicationContext.getBean(properties.getServer(), MBeanServer.class));
					super.afterSingletonsInstantiated();
				}

			};
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
	@ConditionalOnClass({ JdbcMessageStore.class, DataSourceScriptDatabaseInitializer.class })
	@ConditionalOnSingleCandidate(DataSource.class)
	@Conditional(OnIntegrationDatasourceInitializationCondition.class)
	protected static class IntegrationJdbcConfiguration {

		@Bean
		@ConditionalOnMissingBean
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

			AnyRSocketChannelAdapterAvailable() {
				super(ConfigurationPhase.REGISTER_BEAN);
			}

			@ConditionalOnBean(IntegrationRSocketEndpoint.class)
			static class IntegrationRSocketEndpointAvailable {

			}

			@ConditionalOnBean(RSocketOutboundGateway.class)
			static class RSocketOutboundGatewayAvailable {

			}

		}

		@Configuration(proxyBeanMethods = false)
		@ConditionalOnClass(TcpServerTransport.class)
		protected static class IntegrationRSocketServerConfiguration {

			@Bean
			@ConditionalOnMissingBean(ServerRSocketMessageHandler.class)
			public RSocketMessageHandler serverRSocketMessageHandler(RSocketStrategies rSocketStrategies,
					IntegrationProperties integrationProperties) {

				RSocketMessageHandler messageHandler = new ServerRSocketMessageHandler(
						integrationProperties.getRsocket().getServer().isMessageMappingEnabled());
				messageHandler.setRSocketStrategies(rSocketStrategies);
				return messageHandler;
			}

			@Bean
			@ConditionalOnMissingBean
			public ServerRSocketConnector serverRSocketConnector(ServerRSocketMessageHandler messageHandler) {
				return new ServerRSocketConnector(messageHandler);
			}

		}

		@Configuration(proxyBeanMethods = false)
		protected static class IntegrationRSocketClientConfiguration {

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

				RemoteRSocketServerAddressConfigured() {
					super(ConfigurationPhase.REGISTER_BEAN);
				}

				@ConditionalOnProperty("spring.integration.rsocket.client.uri")
				static class WebSocketAddressConfigured {

				}

				@ConditionalOnProperty({ "spring.integration.rsocket.client.host",
						"spring.integration.rsocket.client.port" })
				static class TcpAddressConfigured {

				}

			}

		}

	}

	static class OnIntegrationDatasourceInitializationCondition extends OnDatabaseInitializationCondition {

		OnIntegrationDatasourceInitializationCondition() {
			super("Integration", "spring.integration.jdbc.initialize-schema");
		}

	}

}
