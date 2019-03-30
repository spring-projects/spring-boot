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

package org.springframework.boot.actuate.autoconfigure.context.properties;

import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnEnabledEndpoint;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnExposedEndpoint;
import org.springframework.boot.actuate.context.properties.ConfigurationPropertiesReportEndpoint;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
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
 * @since 2.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnEnabledEndpoint(endpoint = ConfigurationPropertiesReportEndpoint.class)
@ConditionalOnExposedEndpoint(endpoint = ConfigurationPropertiesReportEndpoint.class)
@EnableConfigurationProperties(ConfigurationPropertiesReportEndpointProperties.class)
public class ConfigurationPropertiesReportEndpointAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public ConfigurationPropertiesReportEndpoint configurationPropertiesReportEndpoint(
			ConfigurationPropertiesReportEndpointProperties properties) {
		ConfigurationPropertiesReportEndpoint endpoint = new ConfigurationPropertiesReportEndpoint();
		String[] keysToSanitize = properties.getKeysToSanitize();
		if (keysToSanitize != null) {
			endpoint.setKeysToSanitize(keysToSanitize);
		}
		return endpoint;
	}

}
