/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.health.contributor;

/**
 * Adapts a {@link ReactiveHealthIndicator} to a {@link HealthIndicator}.
 *
 * @author Phillip Webb
 * @see ReactiveHealthIndicator#asHealthContributor()
 */
class ReactiveHealthIndicatorAdapter implements HealthIndicator {

	private final ReactiveHealthIndicator delegate;

	ReactiveHealthIndicatorAdapter(ReactiveHealthIndicator indicator) {
		this.delegate = indicator;
	}

	@Override
	public Health health(boolean includeDetails) {
		return this.delegate.health(includeDetails).block();
	}

	@Override
	public Health health() {
		return this.delegate.health().block();
	}

}
