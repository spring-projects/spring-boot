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

package org.springframework.boot.actuate.health;

import reactor.core.publisher.Mono;

/**
 * Base {@link ReactiveHealthIndicator} implementations that encapsulates creation of
 * {@link Health} instance and error handling.
 *
 * @author Stephane Nicoll
 * @author Nikolay Rybak
 * @since 2.0.0
 */
public abstract class AbstractReactiveHealthIndicator implements ReactiveHealthIndicator {

	@Override
	public final Mono<Health> health() {
		try {
			return doHealthCheck(new Health.Builder()).onErrorResume(this::handleFailure);
		}
		catch (Exception ex) {
			return handleFailure(ex);
		}
	}

	private Mono<Health> handleFailure(Throwable ex) {
		return Mono.just(new Health.Builder().down(ex).build());
	}

	/**
	 * Actual health check logic. If an error occurs in the pipeline it will be handled
	 * automatically.
	 * @param builder the {@link Health.Builder} to report health status and details
	 * @return a {@link Mono} that provides the {@link Health}
	 */
	protected abstract Mono<Health> doHealthCheck(Health.Builder builder);

}
