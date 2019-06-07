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
import javax.sql.DataSource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.config.EnableIntegrationManagement;
import org.springframework.integration.gateway.GatewayProxyFactoryBean;
import org.springframework.integration.jdbc.store.JdbcMessageStore;
import org.springframework.integration.jmx.config.EnableIntegrationMBeanExport;
import org.springframework.integration.monitor.IntegrationMBeanExporter;
import org.springframework.integration.support.management.IntegrationManagementConfigurer;
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
@Configuration
@ConditionalOnClass(EnableIntegration.class)
@EnableConfigurationProperties(IntegrationProperties.class)
@AutoConfigureAfter({ DataSourceAutoConfiguration.class, JmxAutoConfiguration.class })
public class IntegrationAutoConfiguration {

	/**
	 * Basic Spring Integration configuration.
	 */
	@Configuration
	@EnableIntegration
	protected static class IntegrationConfiguration {

	}

	/**
	 * Spring Integration JMX configuration.
	 */
	@Configuration
	@ConditionalOnClass(EnableIntegrationMBeanExport.class)
	@ConditionalOnMissingBean(value = IntegrationMBeanExporter.class, search = SearchStrategy.CURRENT)
	@ConditionalOnBean(MBeanServer.class)
	@ConditionalOnProperty(prefix = "spring.jmx", name = "enabled", havingValue = "true", matchIfMissing = true)
	protected static class IntegrationJmxConfiguration implements EnvironmentAware, BeanFactoryAware {

		private BeanFactory beanFactory;

		private Environment environment;

		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			this.beanFactory = beanFactory;
		}

		@Override
		public void setEnvironment(Environment environment) {
			this.environment = environment;
		}

		@Bean
		public IntegrationMBeanExporter integrationMbeanExporter() {
			IntegrationMBeanExporter exporter = new IntegrationMBeanExporter();
			String defaultDomain = this.environment.getProperty("spring.jmx.default-domain");
			if (StringUtils.hasLength(defaultDomain)) {
				exporter.setDefaultDomain(defaultDomain);
			}
			String serverBean = this.environment.getProperty("spring.jmx.server", "mbeanServer");
			exporter.setServer(this.beanFactory.getBean(serverBean, MBeanServer.class));
			return exporter;
		}

	}

	/**
	 * Integration management configuration.
	 */
	@Configuration
	@ConditionalOnClass(EnableIntegrationManagement.class)
	@ConditionalOnMissingBean(value = IntegrationManagementConfigurer.class,
			name = IntegrationManagementConfigurer.MANAGEMENT_CONFIGURER_NAME, search = SearchStrategy.CURRENT)
	protected static class IntegrationManagementConfiguration {

		@Configuration
		@EnableIntegrationManagement(defaultCountsEnabled = "true")
		protected static class EnableIntegrationManagementConfiguration {

		}

	}

	/**
	 * Integration component scan configuration.
	 */
	@Configuration
	@ConditionalOnMissingBean(GatewayProxyFactoryBean.class)
	@Import(IntegrationAutoConfigurationScanRegistrar.class)
	protected static class IntegrationComponentScanConfiguration {

	}

	/**
	 * Integration JDBC configuration.
	 */
	@Configuration
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

}
