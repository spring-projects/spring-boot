/*
 * Copyright 2012-2020 the original author or authors.
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

import org.springframework.boot.actuate.health.StatusAggregator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration to adapt legacy deprecated health endpoint classes and interfaces.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @see HealthEndpointAutoConfiguration
 */
@Configuration(proxyBeanMethods = false)
@SuppressWarnings("deprecation")
class LegacyHealthEndpointAdaptersConfiguration {

	@Bean
	@ConditionalOnBean(org.springframework.boot.actuate.health.HealthAggregator.class)
	StatusAggregator healthAggregatorStatusAggregatorAdapter(
			org.springframework.boot.actuate.health.HealthAggregator healthAggregator) {
		return new HealthAggregatorStatusAggregatorAdapter(healthAggregator);
	}

}
