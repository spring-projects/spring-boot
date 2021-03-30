/*
 * Copyright 2012-2021 the original author or authors.
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
import reactor.core.scheduler.Schedulers;

import org.springframework.util.Assert;

/**
 * Adapts a {@link HealthIndicator} to a {@link ReactiveHealthIndicator} so that it can be
 * safely invoked in a reactive environment.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 * @deprecated since 2.2.0 for removal in 2.4.0 in favor of
 * {@link ReactiveHealthContributor#adapt(HealthContributor)}
 * @see ReactiveHealthContributor#adapt(HealthContributor)
 */
@Deprecated
public class HealthIndicatorReactiveAdapter implements ReactiveHealthIndicator {

	private final HealthIndicator delegate;

	public HealthIndicatorReactiveAdapter(HealthIndicator delegate) {
		Assert.notNull(delegate, "Delegate must not be null");
		this.delegate = delegate;
	}

	@Override
	public Mono<Health> health() {
		return Mono.fromCallable(this.delegate::health).subscribeOn(Schedulers.boundedElastic());
	}

}
