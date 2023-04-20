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

import org.springframework.util.Assert;

/**
 * Tagging interface for classes that contribute to {@link HealthComponent health
 * components} to the results returned from the {@link HealthEndpoint}. A contributor must
 * be either a {@link ReactiveHealthIndicator} or a
 * {@link CompositeReactiveHealthContributor}.
 *
 * @author Phillip Webb
 * @since 2.2.0
 * @see ReactiveHealthIndicator
 * @see CompositeReactiveHealthContributor
 */
public interface ReactiveHealthContributor {

	static ReactiveHealthContributor adapt(HealthContributor healthContributor) {
		Assert.notNull(healthContributor, "HealthContributor must not be null");
		if (healthContributor instanceof HealthIndicator healthIndicator) {
			return new HealthIndicatorReactiveAdapter(healthIndicator);
		}
		if (healthContributor instanceof CompositeHealthContributor compositeHealthContributor) {
			return new CompositeHealthContributorReactiveAdapter(compositeHealthContributor);
		}
		throw new IllegalStateException("Unknown HealthContributor type");
	}

}
