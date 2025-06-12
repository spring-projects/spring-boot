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

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import reactor.core.publisher.Mono;

import org.springframework.boot.health.contributor.ReactiveHealthContributors.Entry;

/**
 * Tests for {@link MapCompositeReactiveHealthContributor}.
 *
 * @author Phillip Webb
 */
class MapCompositeReactiveHealthContributorTests
		extends MapCompositeTests<CompositeReactiveHealthContributor, ReactiveHealthContributor, Entry> {

	@Override
	protected CompositeReactiveHealthContributor create(Map<String, String> map,
			Function<String, ReactiveHealthContributor> valueAdapter) {
		return new MapCompositeReactiveHealthContributor<>(map, valueAdapter);
	}

	@Override
	protected Stream<Entry> stream(CompositeReactiveHealthContributor composite) {
		return composite.stream();
	}

	@Override
	protected ReactiveHealthContributor getContributor(CompositeReactiveHealthContributor composite, String name) {
		return composite.getContributor(name);
	}

	@Override
	protected ReactiveHealthContributor createContributor(String data) {
		return (ReactiveHealthIndicator) () -> Mono.just(Health.up().withDetail("data", data).build());
	}

	@Override
	protected String getData(ReactiveHealthContributor contributor) {
		return (String) ((ReactiveHealthIndicator) contributor).health().block().getDetails().get("data");
	}

	@Override
	protected String getName(Entry entry) {
		return entry.name();
	}

	@Override
	protected ReactiveHealthContributor getContributor(Entry entry) {
		return entry.contributor();
	}

}
