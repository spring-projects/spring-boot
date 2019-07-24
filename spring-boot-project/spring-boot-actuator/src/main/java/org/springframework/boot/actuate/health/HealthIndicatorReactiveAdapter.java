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
import reactor.core.publisher.MonoSink;
import reactor.core.scheduler.Schedulers;

import org.springframework.util.Assert;

/**
 * Adapts a {@link HealthIndicator} to a {@link ReactiveHealthIndicator} so that it can be
 * safely invoked in a reactive environment.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class HealthIndicatorReactiveAdapter implements ReactiveHealthIndicator {

	private final HealthIndicator delegate;

	public HealthIndicatorReactiveAdapter(HealthIndicator delegate) {
		Assert.notNull(delegate, "Delegate must not be null");
		this.delegate = delegate;
	}

	@Override
	public Mono<Health> health() {
		return Mono.create((sink) -> Schedulers.elastic().schedule(() -> invoke(sink)));
	}

	private void invoke(MonoSink<Health> sink) {
		try {
			Health health = this.delegate.health();
			sink.success(health);
		}
		catch (Exception ex) {
			sink.error(ex);
		}
	}

}
