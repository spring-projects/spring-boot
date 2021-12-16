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

package org.springframework.boot.actuate.autoconfigure.context.properties;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.autoconfigure.endpoint.expose.EndpointExposure;
import org.springframework.boot.actuate.context.properties.ConfigurationPropertiesReportEndpoint;
import org.springframework.boot.actuate.context.properties.ConfigurationPropertiesReportEndpointWebExtension;
import org.springframework.boot.actuate.endpoint.SanitizingFunction;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the
 * {@link ConfigurationPropertiesReportEndpoint}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Chris Bono
 * @since 2.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnAvailableEndpoint(endpoint = ConfigurationPropertiesReportEndpoint.class)
@EnableConfigurationProperties(ConfigurationPropertiesReportEndpointProperties.class)
public class ConfigurationPropertiesReportEndpointAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public ConfigurationPropertiesReportEndpoint configurationPropertiesReportEndpoint(
			ConfigurationPropertiesReportEndpointProperties properties,
			ObjectProvider<SanitizingFunction> sanitizingFunctions) {
		ConfigurationPropertiesReportEndpoint endpoint = new ConfigurationPropertiesReportEndpoint(sanitizingFunctions);
		String[] keysToSanitize = properties.getKeysToSanitize();
		if (keysToSanitize != null) {
			endpoint.setKeysToSanitize(keysToSanitize);
		}
		String[] additionalKeysToSanitize = properties.getAdditionalKeysToSanitize();
		if (additionalKeysToSanitize != null) {
			endpoint.keysToSanitize(additionalKeysToSanitize);
		}
		return endpoint;
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean(ConfigurationPropertiesReportEndpoint.class)
	@ConditionalOnAvailableEndpoint(exposure = EndpointExposure.WEB)
	public ConfigurationPropertiesReportEndpointWebExtension configurationPropertiesReportEndpointWebExtension(
			ConfigurationPropertiesReportEndpoint configurationPropertiesReportEndpoint) {
		return new ConfigurationPropertiesReportEndpointWebExtension(configurationPropertiesReportEndpoint);
	}

}
