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

package org.springframework.boot.actuate.autoconfigure.env;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.autoconfigure.endpoint.expose.EndpointExposure;
import org.springframework.boot.actuate.endpoint.SanitizingFunction;
import org.springframework.boot.actuate.env.EnvironmentEndpoint;
import org.springframework.boot.actuate.env.EnvironmentEndpointWebExtension;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the {@link EnvironmentEndpoint}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@AutoConfiguration
@ConditionalOnAvailableEndpoint(endpoint = EnvironmentEndpoint.class)
@EnableConfigurationProperties(EnvironmentEndpointProperties.class)
public class EnvironmentEndpointAutoConfiguration {

	/**
     * Creates an instance of the {@link EnvironmentEndpoint} class if no other bean of the same type is present.
     * 
     * @param environment The {@link Environment} object used to retrieve environment information.
     * @param properties The {@link EnvironmentEndpointProperties} object containing endpoint properties.
     * @param sanitizingFunctions The {@link ObjectProvider} of {@link SanitizingFunction} used for sanitizing environment values.
     * @return An instance of the {@link EnvironmentEndpoint} class.
     */
    @Bean
	@ConditionalOnMissingBean
	public EnvironmentEndpoint environmentEndpoint(Environment environment, EnvironmentEndpointProperties properties,
			ObjectProvider<SanitizingFunction> sanitizingFunctions) {
		return new EnvironmentEndpoint(environment, sanitizingFunctions.orderedStream().toList(),
				properties.getShowValues());
	}

	/**
     * Creates a new instance of {@link EnvironmentEndpointWebExtension} if no other bean of the same type is present in the application context.
     * This bean is conditionally created based on the presence of a bean of type {@link EnvironmentEndpoint} and the availability of the endpoint for exposure.
     * The {@link EnvironmentEndpointWebExtension} is created with the provided {@link EnvironmentEndpoint} bean, the value visibility flag from {@link EnvironmentEndpointProperties},
     * and the roles from {@link EnvironmentEndpointProperties}.
     *
     * @param environmentEndpoint the {@link EnvironmentEndpoint} bean to be used by the {@link EnvironmentEndpointWebExtension}
     * @param properties the {@link EnvironmentEndpointProperties} containing the configuration properties for the endpoint
     * @return a new instance of {@link EnvironmentEndpointWebExtension} if the conditions are met, otherwise null
     */
    @Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean(EnvironmentEndpoint.class)
	@ConditionalOnAvailableEndpoint(exposure = { EndpointExposure.WEB, EndpointExposure.CLOUD_FOUNDRY })
	public EnvironmentEndpointWebExtension environmentEndpointWebExtension(EnvironmentEndpoint environmentEndpoint,
			EnvironmentEndpointProperties properties) {
		return new EnvironmentEndpointWebExtension(environmentEndpoint, properties.getShowValues(),
				properties.getRoles());
	}

}
