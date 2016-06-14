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

import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import javax.management.MBeanServer;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.http.config.EnableIntegrationGraphController;
import org.springframework.integration.http.management.IntegrationGraphController;
import org.springframework.integration.http.support.HttpContextUtils;
import org.springframework.integration.jmx.config.EnableIntegrationMBeanExport;
import org.springframework.integration.monitor.IntegrationMBeanExporter;
import org.springframework.integration.support.management.IntegrationManagementConfigurer;
import org.springframework.integration.support.management.graph.IntegrationGraphServer;
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
@EnableConfigurationProperties(IntegrationProperties.class)
public class IntegrationAutoConfiguration {

	@Configuration
	@EnableIntegration
	protected static class IntegrationConfiguration {

		@Bean(name = IntegrationContextUtils.INTEGRATION_GLOBAL_PROPERTIES_BEAN_NAME)
		@ConditionalOnMissingBean(name = IntegrationContextUtils.INTEGRATION_GLOBAL_PROPERTIES_BEAN_NAME)
		public Properties integrationGlobalProperties(IntegrationProperties integrationProperties) {
			Properties properties = new Properties();
			properties.setProperty(org.springframework.integration.context.IntegrationProperties.CHANNELS_AUTOCREATE,
					"" + integrationProperties.getChannels().isAutoCreate());
			properties.setProperty(org.springframework.integration.context.IntegrationProperties.CHANNELS_MAX_UNICAST_SUBSCRIBERS,
					Integer.toString(integrationProperties.getChannels().getMaxUnicastSubscribers()));
			properties.setProperty(org.springframework.integration.context.IntegrationProperties.CHANNELS_MAX_BROADCAST_SUBSCRIBERS,
					Integer.toString(integrationProperties.getChannels().getMaxBroadcastSubscribers()));
			properties.setProperty(org.springframework.integration.context.IntegrationProperties.TASK_SCHEDULER_POOL_SIZE,
					Integer.toString(integrationProperties.getTaskSchedulerPoolSize()));
			properties.setProperty(org.springframework.integration.context.IntegrationProperties.THROW_EXCEPTION_ON_LATE_REPLY,
					"" + integrationProperties.isThrowExceptionOnLateReply());
			properties.setProperty(org.springframework.integration.context.IntegrationProperties.REQUIRE_COMPONENT_ANNOTATION,
					"" + integrationProperties.isComponentAnnotationRequired());
			return properties;
		}

	}

	@Configuration
	@ConditionalOnClass(EnableIntegrationMBeanExport.class)
	@ConditionalOnMissingBean(value = IntegrationMBeanExporter.class, search = SearchStrategy.CURRENT)
	@ConditionalOnProperty(prefix = "spring.jmx", name = "enabled", havingValue = "true", matchIfMissing = true)
	protected static class IntegrationJmxConfiguration implements EnvironmentAware, BeanFactoryAware {

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
			this.propertyResolver = new RelaxedPropertyResolver(environment, "spring.jmx.");
		}

		@Bean
		@Primary
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

	@Configuration
	@ConditionalOnWebApplication
	@ConditionalOnClass(EnableIntegrationGraphController.class)
	@ConditionalOnMissingBean(value = IntegrationGraphController.class)
	protected static class IntegrationGraphControllerConfiguration
			implements BeanFactoryPostProcessor, EnvironmentAware {

		@Bean
		@ConditionalOnMissingBean(value = IntegrationGraphServer.class)
		public IntegrationGraphServer integrationGraphServer() {
			return new IntegrationGraphServer();
		}

		@Bean
		public IntegrationGraphController integrationGraphController(IntegrationGraphServer integrationGraphServer) {
			return new IntegrationGraphController(integrationGraphServer);
		}

		@Override
		public void setEnvironment(Environment environment) {
			RelaxedPropertyResolver propertyResolver = new RelaxedPropertyResolver(environment, "spring.integration.");
			String graphControllerPath = propertyResolver.getProperty("graph-controller-path",
					HttpContextUtils.GRAPH_CONTROLLER_DEFAULT_PATH);
			Map<String, Object> graphControllerProperties =
					Collections.<String, Object>singletonMap(HttpContextUtils.GRAPH_CONTROLLER_PATH_PROPERTY,
							graphControllerPath);
			((ConfigurableEnvironment) environment)
					.getPropertySources()
					.addLast(new MapPropertySource(HttpContextUtils.GRAPH_CONTROLLER_BEAN_NAME + "_properties",
							graphControllerProperties));
		}

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

		}

	}

}
