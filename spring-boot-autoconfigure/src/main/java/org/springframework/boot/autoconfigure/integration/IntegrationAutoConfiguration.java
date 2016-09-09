/*
 * Copyright 2012-2016 the original author or authors.
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

import javax.management.MBeanServer;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.integration.config.EnableIntegration;
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
 * @since 1.1.0
 */
@Configuration
@ConditionalOnClass(EnableIntegration.class)
@AutoConfigureAfter(JmxAutoConfiguration.class)
public class IntegrationAutoConfiguration {

	@Configuration
	@EnableIntegration
	protected static class IntegrationConfiguration {
	}

	@Configuration
	@ConditionalOnClass(EnableIntegrationMBeanExport.class)
	@ConditionalOnMissingBean(value = IntegrationMBeanExporter.class, search = SearchStrategy.CURRENT)
	@ConditionalOnProperty(prefix = "spring.jmx", name = "enabled", havingValue = "true", matchIfMissing = true)
	protected static class IntegrationJmxConfiguration
			implements EnvironmentAware, BeanFactoryAware {

		private BeanFactory beanFactory;

		private RelaxedPropertyResolver propertyResolver;

		@Autowired(required = false)
		@Qualifier(IntegrationManagementConfigurer.MANAGEMENT_CONFIGURER_NAME)
		private IntegrationManagementConfigurer configurer;

		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			this.beanFactory = beanFactory;
		}

		@Override
		public void setEnvironment(Environment environment) {
			this.propertyResolver = new RelaxedPropertyResolver(environment,
					"spring.jmx.");
		}

		@Bean
		public IntegrationMBeanExporter integrationMbeanExporter() {
			IntegrationMBeanExporter exporter = new IntegrationMBeanExporter();
			String defaultDomain = this.propertyResolver.getProperty("default-domain");
			if (StringUtils.hasLength(defaultDomain)) {
				exporter.setDefaultDomain(defaultDomain);
			}
			String server = this.propertyResolver.getProperty("server", "mbeanServer");
			if (StringUtils.hasLength(server)) {
				exporter.setServer(this.beanFactory.getBean(server, MBeanServer.class));
			}
			if (this.configurer != null) {
				if (this.configurer.getDefaultCountsEnabled() == null) {
					this.configurer.setDefaultCountsEnabled(true);
				}
				if (this.configurer.getDefaultStatsEnabled() == null) {
					this.configurer.setDefaultStatsEnabled(true);
				}
			}
			return exporter;
		}

	}

}
