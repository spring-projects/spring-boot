/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.sbom;

import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.autoconfigure.endpoint.expose.EndpointExposure;
import org.springframework.boot.actuate.sbom.SbomEndpoint;
import org.springframework.boot.actuate.sbom.SbomEndpointWebExtension;
import org.springframework.boot.actuate.sbom.SbomProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link SbomEndpoint}.
 *
 * @author Moritz Halbritter
 * @since 3.3.0
 */
@AutoConfiguration
@ConditionalOnAvailableEndpoint(SbomEndpoint.class)
@EnableConfigurationProperties(SbomProperties.class)
public class SbomEndpointAutoConfiguration {

	private final SbomProperties properties;

	SbomEndpointAutoConfiguration(SbomProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	SbomEndpoint sbomEndpoint(ResourceLoader resourceLoader) {
		return new SbomEndpoint(this.properties, resourceLoader);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean(SbomEndpoint.class)
	@ConditionalOnAvailableEndpoint(exposure = EndpointExposure.WEB)
	SbomEndpointWebExtension sbomEndpointWebExtension(SbomEndpoint sbomEndpoint) {
		return new SbomEndpointWebExtension(sbomEndpoint, this.properties);
	}

}
