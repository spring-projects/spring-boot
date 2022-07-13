/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.web.server;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextFactory;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextType;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.util.Assert;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the management context. If the
 * {@code management.server.port} is the same as the {@code server.port} the management
 * context will be the same as the main application context. If the
 * {@code management.server.port} is different to the {@code server.port} the management
 * context will be a separate context that has the main application context as its parent.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
@AutoConfiguration
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
@EnableConfigurationProperties({ WebEndpointProperties.class, ManagementServerProperties.class })
public class ManagementContextAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnManagementPort(ManagementPortType.SAME)
	static class SameManagementContextConfiguration implements SmartInitializingSingleton {

		private final Environment environment;

		SameManagementContextConfiguration(Environment environment) {
			this.environment = environment;
		}

		@Override
		public void afterSingletonsInstantiated() {
			verifySslConfiguration();
			verifyAddressConfiguration();
			if (this.environment instanceof ConfigurableEnvironment configurableEnvironment) {
				addLocalManagementPortPropertyAlias(configurableEnvironment);
			}
		}

		private void verifySslConfiguration() {
			Boolean enabled = this.environment.getProperty("management.server.ssl.enabled", Boolean.class, false);
			Assert.state(!enabled, "Management-specific SSL cannot be configured as the management "
					+ "server is not listening on a separate port");
		}

		private void verifyAddressConfiguration() {
			Object address = this.environment.getProperty("management.server.address");
			Assert.state(address == null, "Management-specific server address cannot be configured as the management "
					+ "server is not listening on a separate port");
		}

		/**
		 * Add an alias for 'local.management.port' that actually resolves using
		 * 'local.server.port'.
		 * @param environment the environment
		 */
		private void addLocalManagementPortPropertyAlias(ConfigurableEnvironment environment) {
			environment.getPropertySources().addLast(new PropertySource<>("Management Server") {

				@Override
				public Object getProperty(String name) {
					if ("local.management.port".equals(name)) {
						return environment.getProperty("local.server.port");
					}
					return null;
				}

			});
		}

		@Configuration(proxyBeanMethods = false)
		@EnableManagementContext(ManagementContextType.SAME)
		static class EnableSameManagementContextConfiguration {

		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnManagementPort(ManagementPortType.DIFFERENT)
	static class DifferentManagementContextConfiguration {

		@Bean
		ChildManagementContextInitializer childManagementContextInitializer(
				ManagementContextFactory managementContextFactory, ApplicationContext parentContext) {
			return new ChildManagementContextInitializer(managementContextFactory, parentContext);
		}

	}

}
