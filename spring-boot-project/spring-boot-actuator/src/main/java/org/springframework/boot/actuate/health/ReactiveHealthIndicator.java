/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
 * Defines the {@link Health} of an arbitrary system or component.
 * <p>
 * This is non blocking contract that is meant to be used in a reactive application. See
 * {@link HealthIndicator} for the traditional contract.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 * @see HealthIndicator
 */
@FunctionalInterface
public interface ReactiveHealthIndicator {

	/**
	 * Provide the indicator of health.
	 * @return a {@link Mono} that provides the {@link Health}
	 */
	Mono<Health> health();

}
