/*
 * Copyright 2012-2023 the original author or authors.
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
import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.WebServerNamespace;
import org.springframework.boot.actuate.endpoint.web.reactive.AdditionalHealthEndpointPathsWebFluxHandlerMapping;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.HealthEndpointGroups;
import org.springframework.boot.actuate.health.ReactiveHealthContributorRegistry;
import org.springframework.boot.actuate.health.ReactiveHealthEndpointWebExtension;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for {@link HealthEndpoint} reactive web extensions.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @see HealthEndpointAutoConfiguration
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = Type.REACTIVE)
@ConditionalOnAvailableEndpoint(endpoint = HealthEndpoint.class,
		exposure = { EndpointExposure.WEB, EndpointExposure.CLOUD_FOUNDRY })
class HealthEndpointReactiveWebExtensionConfiguration {

	/**
     * Creates a new instance of ReactiveHealthEndpointWebExtension.
     * This method is annotated with @Bean, @ConditionalOnMissingBean, and @ConditionalOnBean(HealthEndpoint.class),
     * indicating that it will be used to create a bean if no other bean of the same type is present,
     * and only if a bean of type HealthEndpoint is present.
     * 
     * @param reactiveHealthContributorRegistry The ReactiveHealthContributorRegistry used to retrieve health contributors.
     * @param groups The HealthEndpointGroups used to group health contributors.
     * @param properties The HealthEndpointProperties used to configure the health endpoint.
     * @return A new instance of ReactiveHealthEndpointWebExtension.
     */
    @Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean(HealthEndpoint.class)
	ReactiveHealthEndpointWebExtension reactiveHealthEndpointWebExtension(
			ReactiveHealthContributorRegistry reactiveHealthContributorRegistry, HealthEndpointGroups groups,
			HealthEndpointProperties properties) {
		return new ReactiveHealthEndpointWebExtension(reactiveHealthContributorRegistry, groups,
				properties.getLogging().getSlowIndicatorThreshold());
	}

	/**
     * WebFluxAdditionalHealthEndpointPathsConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnAvailableEndpoint(endpoint = HealthEndpoint.class, exposure = EndpointExposure.WEB)
	static class WebFluxAdditionalHealthEndpointPathsConfiguration {

		/**
         * Creates a WebFluxHandlerMapping for additional health endpoint paths.
         * 
         * @param webEndpointsSupplier the supplier of web endpoints
         * @param groups the health endpoint groups
         * @return the WebFluxHandlerMapping for additional health endpoint paths
         * @throws IllegalStateException if no endpoint with the specified ID is found
         */
        @Bean
		AdditionalHealthEndpointPathsWebFluxHandlerMapping healthEndpointWebFluxHandlerMapping(
				WebEndpointsSupplier webEndpointsSupplier, HealthEndpointGroups groups) {
			Collection<ExposableWebEndpoint> webEndpoints = webEndpointsSupplier.getEndpoints();
			ExposableWebEndpoint health = webEndpoints.stream()
				.filter((endpoint) -> endpoint.getEndpointId().equals(HealthEndpoint.ID))
				.findFirst()
				.orElseThrow(
						() -> new IllegalStateException("No endpoint with id '%s' found".formatted(HealthEndpoint.ID)));
			return new AdditionalHealthEndpointPathsWebFluxHandlerMapping(new EndpointMapping(""), health,
					groups.getAllWithAdditionalPath(WebServerNamespace.SERVER));
		}

	}

}
