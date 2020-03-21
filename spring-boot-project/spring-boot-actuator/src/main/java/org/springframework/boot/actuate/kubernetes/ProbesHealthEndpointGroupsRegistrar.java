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

package org.springframework.boot.actuate.kubernetes;

import org.springframework.boot.actuate.health.HealthEndpointGroup;
import org.springframework.boot.actuate.health.HealthEndpointGroupsRegistry;
import org.springframework.boot.actuate.health.HealthEndpointGroupsRegistryCustomizer;

/**
 * {@link HealthEndpointGroupsRegistryCustomizer} that registers {@code "liveness"} and
 * {@code "readiness"} {@link HealthEndpointGroup groups} if they don't exist already.
 *
 * @author Brian Clozel
 * @since 2.3.0
 */
public class ProbesHealthEndpointGroupsRegistrar implements HealthEndpointGroupsRegistryCustomizer {

	private static final String LIVENESS_GROUP_NAME = "liveness";

	private static final String READINESS_GROUP_NAME = "readiness";

	private static final String LIVENESS_PROBE_INDICATOR = "livenessProbe";

	private static final String READINESS_PROBE_INDICATOR = "readinessProbe";

	@Override
	public void customize(HealthEndpointGroupsRegistry registry) {
		if (registry.get(LIVENESS_GROUP_NAME) == null) {
			registry.add(LIVENESS_GROUP_NAME, (configurer) -> configurer.include(LIVENESS_PROBE_INDICATOR));
		}
		if (registry.get(READINESS_GROUP_NAME) == null) {
			registry.add(READINESS_GROUP_NAME, (configurer) -> configurer.include(READINESS_PROBE_INDICATOR));
		}
	}

}
