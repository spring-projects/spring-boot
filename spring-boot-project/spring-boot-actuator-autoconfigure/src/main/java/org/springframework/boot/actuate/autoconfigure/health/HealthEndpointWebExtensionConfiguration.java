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

package org.springframework.boot.actuate.autoconfigure.health;

import java.util.Collection;

import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.autoconfigure.endpoint.expose.EndpointExposure;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.WebServerNamespace;
import org.springframework.boot.actuate.endpoint.web.servlet.AdditionalHealthEndpointPathsWebMvcHandlerMapping;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.HealthEndpointGroups;
import org.springframework.boot.actuate.health.HealthEndpointWebExtension;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Configuration for {@link HealthEndpoint} web extensions.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @see HealthEndpointAutoConfiguration
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnBean(HealthEndpoint.class)
@ConditionalOnAvailableEndpoint(endpoint = HealthEndpoint.class,
		exposure = { EndpointExposure.WEB, EndpointExposure.CLOUD_FOUNDRY })
class HealthEndpointWebExtensionConfiguration {

	@Bean
	@ConditionalOnMissingBean
	HealthEndpointWebExtension healthEndpointWebExtension(HealthContributorRegistry healthContributorRegistry,
			HealthEndpointGroups groups, HealthEndpointProperties properties) {
		return new HealthEndpointWebExtension(healthContributorRegistry, groups,
				properties.getLogging().getSlowIndicatorThreshold());
	}

	private static ExposableWebEndpoint getHealthEndpoint(WebEndpointsSupplier webEndpointsSupplier) {
		Collection<ExposableWebEndpoint> webEndpoints = webEndpointsSupplier.getEndpoints();
		return webEndpoints.stream().filter((endpoint) -> endpoint.getEndpointId().equals(HealthEndpoint.ID))
				.findFirst().get();
	}

	@ConditionalOnBean(DispatcherServlet.class)
	@ConditionalOnAvailableEndpoint(endpoint = HealthEndpoint.class, exposure = EndpointExposure.WEB)
	static class MvcAdditionalHealthEndpointPathsConfiguration {

		@Bean
		AdditionalHealthEndpointPathsWebMvcHandlerMapping healthEndpointWebMvcHandlerMapping(
				WebEndpointsSupplier webEndpointsSupplier, HealthEndpointGroups groups) {
			ExposableWebEndpoint health = getHealthEndpoint(webEndpointsSupplier);
			return new AdditionalHealthEndpointPathsWebMvcHandlerMapping(health,
					groups.getAllWithAdditionalPath(WebServerNamespace.SERVER));
		}

	}

}
