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

import java.util.Properties;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.http.config.EnableIntegrationGraphController;
import org.springframework.integration.http.management.IntegrationGraphController;
import org.springframework.integration.jmx.config.EnableIntegrationMBeanExport;
import org.springframework.integration.monitor.IntegrationMBeanExporter;

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
	@EnableIntegrationMBeanExport(defaultDomain = "${spring.jmx.default-domain:}",
			server = "${spring.jmx.server:mbeanServer}")
	protected static class IntegrationJmxConfiguration {
	}

	@Configuration
	@ConditionalOnWebApplication
	@ConditionalOnClass(EnableIntegrationGraphController.class)
	@ConditionalOnMissingBean(value = IntegrationGraphController.class, search = SearchStrategy.CURRENT)
	@EnableIntegrationGraphController(path = "${spring.integration.graph-controller-path}")
	protected static class IntegrationGraphControllerConfiguration {
	}

}
