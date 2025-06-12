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

import reactor.core.scheduler.Schedulers;

/**
 * Contributes health information, either directly ({@link ReactiveHealthIndicator}) or
 * via other contributors ({@link CompositeReactiveHealthContributor}).
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 4.0.0
 * @see ReactiveHealthIndicator
 * @see CompositeReactiveHealthContributor
 */
public sealed interface ReactiveHealthContributor permits ReactiveHealthIndicator, CompositeReactiveHealthContributor {

	/**
	 * Return this reactive contributor as a standard blocking {@link HealthContributor}.
	 * @return a blocking health contributor
	 */
	HealthContributor asHealthContributor();

	/**
	 * Adapts the given {@link HealthContributor} into a {@link ReactiveHealthContributor}
	 * by scheduling blocking calls to {@link Schedulers#boundedElastic()}.
	 * @param contributor the contributor to adapt or {@code null}
	 * @return the adapted contributor
	 */
	static ReactiveHealthContributor adapt(HealthContributor contributor) {
		if (contributor == null) {
			return null;
		}
		if (contributor instanceof HealthIndicator healthIndicator) {
			return new HealthIndicatorAdapter(healthIndicator);
		}
		if (contributor instanceof CompositeHealthContributor compositeHealthContributor) {
			return new CompositeHealthContributorAdapter(compositeHealthContributor);
		}
		throw new IllegalStateException("Unknown 'contributor' type");
	}

}
