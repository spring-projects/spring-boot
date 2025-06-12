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

import java.util.stream.Stream;

/**
 * Adapts {@link ReactiveHealthContributors} to {@link HealthContributors}.
 *
 * @author Phillip Webb
 * @see ReactiveHealthContributors#asHealthContributors()
 */
class ReactiveHealthContributorsAdapter implements HealthContributors {

	private final ReactiveHealthContributors delegate;

	ReactiveHealthContributorsAdapter(ReactiveHealthContributors delegate) {
		this.delegate = delegate;
	}

	@Override
	public HealthContributor getContributor(String name) {
		return adapt(this.delegate.getContributor(name));
	}

	@Override
	public Stream<Entry> stream() {
		return this.delegate.stream().map((entry) -> new Entry(entry.name(), adapt(entry.contributor())));
	}

	private HealthContributor adapt(ReactiveHealthContributor contributor) {
		return (contributor != null) ? contributor.asHealthContributor() : null;
	}

}
