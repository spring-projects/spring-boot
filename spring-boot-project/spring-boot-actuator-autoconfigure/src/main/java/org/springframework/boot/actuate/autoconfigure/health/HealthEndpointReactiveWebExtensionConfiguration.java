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

package org.springframework.boot.actuate.autoconfigure.health;

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
 * @see HealthEndpointAutoConfiguration
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = Type.REACTIVE)
@ConditionalOnBean(HealthEndpoint.class)
class HealthEndpointReactiveWebExtensionConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean(HealthEndpoint.class)
	ReactiveHealthEndpointWebExtension reactiveHealthEndpointWebExtension(
			ReactiveHealthContributorRegistry reactiveHealthContributorRegistry, HealthEndpointGroups groups) {
		return new ReactiveHealthEndpointWebExtension(reactiveHealthContributorRegistry, groups);
	}

}
