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

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * {@link ReactiveHealthContributors} composed of other
 * {@link ReactiveHealthContributors}.
 *
 * @author Phillip Webb
 * @see ReactiveHealthContributors#of(ReactiveHealthContributors...)
 */
class CompositeReactiveHealthContributors implements ReactiveHealthContributors {

	private final List<ReactiveHealthContributors> contributors;

	CompositeReactiveHealthContributors(ReactiveHealthContributors... contributors) {
		this.contributors = List.of(contributors);
	}

	@Override
	public ReactiveHealthContributor getContributor(String name) {
		return this.contributors.stream()
			.map((reactiveContributors) -> reactiveContributors.getContributor(name))
			.filter(Objects::nonNull)
			.findFirst()
			.orElse(null);
	}

	@Override
	public Stream<Entry> stream() {
		Set<String> seen = new HashSet<>();
		return this.contributors.stream()
			.flatMap(ReactiveHealthContributors::stream)
			.filter((element) -> seen.add(element.name()));
	}

}
