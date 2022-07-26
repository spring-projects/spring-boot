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

package org.springframework.boot.actuate.health;

import reactor.core.publisher.Mono;

/**
 * Strategy interface used to contribute {@link Health} to the results returned from the
 * reactive variant of the {@link HealthEndpoint}.
 * <p>
 * This is non-blocking contract that is meant to be used in a reactive application. See
 * {@link HealthIndicator} for the traditional contract.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 * @see HealthIndicator
 */
@FunctionalInterface
public interface ReactiveHealthIndicator extends ReactiveHealthContributor {

	/**
	 * Provide the indicator of health.
	 * @param includeDetails if details should be included or removed
	 * @return a {@link Mono} that provides the {@link Health}
	 * @since 2.2.0
	 */
	default Mono<Health> getHealth(boolean includeDetails) {
		Mono<Health> health = health();
		return includeDetails ? health : health.map(Health::withoutDetails);
	}

	/**
	 * Provide the indicator of health.
	 * @return a {@link Mono} that provides the {@link Health}
	 */
	Mono<Health> health();

}
