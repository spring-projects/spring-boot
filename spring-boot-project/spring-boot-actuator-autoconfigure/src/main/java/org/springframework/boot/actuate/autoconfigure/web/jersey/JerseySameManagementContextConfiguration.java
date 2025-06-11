/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.web.jersey;

import org.glassfish.jersey.server.ResourceConfig;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.jersey.JerseyProperties;
import org.springframework.boot.autoconfigure.jersey.ResourceConfigCustomizer;
import org.springframework.boot.autoconfigure.web.servlet.DefaultJerseyApplicationPath;
import org.springframework.boot.autoconfigure.web.servlet.JerseyApplicationPath;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * {@link ManagementContextConfiguration @ManagementContextConfiguration} for Jersey
 * infrastructure when the management context is the same as the main application context.
 *
 * @author Madhura Bhave
 * @since 2.1.0
 */
@ManagementContextConfiguration(value = ManagementContextType.SAME, proxyBeanMethods = false)
@EnableConfigurationProperties(JerseyProperties.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass(ResourceConfig.class)
@ConditionalOnMissingClass("org.springframework.web.servlet.DispatcherServlet")
public class JerseySameManagementContextConfiguration {

	@Bean
	ResourceConfigCustomizer managementResourceConfigCustomizerAdapter(
			ObjectProvider<ManagementContextResourceConfigCustomizer> customizers) {
		return (config) -> customizers.orderedStream().forEach((customizer) -> customizer.customize(config));
	}

	@Configuration(proxyBeanMethods = false)
	@Import(JerseyManagementContextConfiguration.class)
	@ConditionalOnMissingBean(ResourceConfig.class)
	static class JerseyInfrastructureConfiguration {

		@Bean
		@ConditionalOnMissingBean
		JerseyApplicationPath jerseyApplicationPath(JerseyProperties properties, ResourceConfig config) {
			return new DefaultJerseyApplicationPath(properties.getApplicationPath(), config);
		}

		@Bean
		ResourceConfig resourceConfig(ObjectProvider<ResourceConfigCustomizer> resourceConfigCustomizers) {
			ResourceConfig resourceConfig = new ResourceConfig();
			resourceConfigCustomizers.orderedStream().forEach((customizer) -> customizer.customize(resourceConfig));
			return resourceConfig;
		}

	}

}
