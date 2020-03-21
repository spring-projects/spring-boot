/*
 * Copyright 2012-2020 the original author or authors.
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
import javax.sql.DataSource;

import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.server.TcpServerTransport;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
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
import org.springframework.boot.autoconfigure.rsocket.RSocketMessagingAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.config.EnableIntegrationManagement;
import org.springframework.integration.config.IntegrationManagementConfigurer;
import org.springframework.integration.gateway.GatewayProxyFactoryBean;
import org.springframework.integration.jdbc.store.JdbcMessageStore;
import org.springframework.integration.jmx.config.EnableIntegrationMBeanExport;
import org.springframework.integration.monitor.IntegrationMBeanExporter;
import org.springframework.integration.rsocket.ClientRSocketConnector;
import org.springframework.integration.rsocket.IntegrationRSocketEndpoint;
import org.springframework.integration.rsocket.ServerRSocketConnector;
import org.springframework.integration.rsocket.ServerRSocketMessageHandler;
import org.springframework.integration.rsocket.outbound.RSocketOutboundGateway;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
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
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(EnableIntegration.class)
@EnableConfigurationProperties(IntegrationProperties.class)
@AutoConfigureAfter({ DataSourceAutoConfiguration.class, JmxAutoConfiguration.class })
public class IntegrationAutoConfiguration {

	/**
	 * Basic Spring Integration configuration.
	 */
	@Configuration(proxyBeanMethods = false)
	@EnableIntegration
	protected static class IntegrationConfiguration {

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

		@Bean
		public IntegrationMBeanExporter integrationMbeanExporter(BeanFactory beanFactory, Environment environment) {
			IntegrationMBeanExporter exporter = new IntegrationMBeanExporter();
			String defaultDomain = environment.getProperty("spring.jmx.default-domain");
			if (StringUtils.hasLength(defaultDomain)) {
				exporter.setDefaultDomain(defaultDomain);
			}
			String serverBean = environment.getProperty("spring.jmx.server", "mbeanServer");
			exporter.setServer(beanFactory.getBean(serverBean, MBeanServer.class));
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

		@Configuration(proxyBeanMethods = false)
		@EnableIntegrationManagement(defaultCountsEnabled = "true")
		protected static class EnableIntegrationManagementConfiguration {

		}

	}

	/**
	 * Integration component scan configuration.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(GatewayProxyFactoryBean.class)
	@Import(IntegrationAutoConfigurationScanRegistrar.class)
	protected static class IntegrationComponentScanConfiguration {

	}

	/**
	 * Integration JDBC configuration.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(JdbcMessageStore.class)
	@ConditionalOnSingleCandidate(DataSource.class)
	protected static class IntegrationJdbcConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public IntegrationDataSourceInitializer integrationDataSourceInitializer(DataSource dataSource,
				ResourceLoader resourceLoader, IntegrationProperties properties) {
			return new IntegrationDataSourceInitializer(dataSource, resourceLoader, properties);
		}

	}

	/**
	 * Integration RSocket configuration.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ IntegrationRSocketEndpoint.class, RSocketRequester.class, RSocketFactory.class })
	@Conditional(IntegrationRSocketConfiguration.AnyRSocketChannelAdapterAvailable.class)
	protected static class IntegrationRSocketConfiguration {

		/**
		 * Check if either a {@link IntegrationRSocketEndpoint} or
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
		@AutoConfigureBefore(RSocketMessagingAutoConfiguration.class)
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

				@ConditionalOnProperty(prefix = "spring.integration.rsocket.client", name = "uri")
				static class WebSocketAddressConfigured {

				}

				@ConditionalOnProperty(prefix = "spring.integration.rsocket.client", name = { "host", "port" })
				static class TcpAddressConfigured {

				}

			}

		}

	}

}
