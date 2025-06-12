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

import java.util.Iterator;
import java.util.stream.Stream;

import reactor.core.scheduler.Schedulers;

import org.springframework.util.Assert;

/**
 * A collection of named {@link ReactiveHealthContributor reactive health contributors}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public interface ReactiveHealthContributors extends Iterable<ReactiveHealthContributors.Entry> {

	/**
	 * Return the contributor with the given name.
	 * @param name the name of the contributor
	 * @return a contributor instance or {@code null}
	 */
	ReactiveHealthContributor getContributor(String name);

	@Override
	default Iterator<Entry> iterator() {
		return stream().iterator();
	}

	/**
	 * Return a stream of the contributor entries.
	 * @return the stream of named contributors
	 */
	Stream<ReactiveHealthContributors.Entry> stream();

	/**
	 * Return these reactive contributors as standard blocking {@link HealthContributors}.
	 * @return blocking health contributors
	 */
	default HealthContributors asHealthContributors() {
		return new ReactiveHealthContributorsAdapter(this);
	}

	/**
	 * Factory method to create a new {@link ReactiveHealthContributors} instance composed
	 * of the given contributors.
	 * @param contributors the source contributors in the order they should be combined
	 * @return a new {@link ReactiveHealthContributors} instance
	 */
	static ReactiveHealthContributors of(ReactiveHealthContributors... contributors) {
		return new CompositeReactiveHealthContributors(contributors);
	}

	/**
	 * Adapts the given {@link HealthContributors} into a
	 * {@link ReactiveHealthContributors} by scheduling blocking calls to
	 * {@link Schedulers#boundedElastic()}.
	 * @param contributors the contributors to adapt or {@code null}
	 * @return the adapted contributor
	 * @see ReactiveHealthContributor#adapt(HealthContributor)
	 */
	static ReactiveHealthContributors adapt(HealthContributors contributors) {
		return (contributors != null) ? new HealthContributorsAdapter(contributors) : null;
	}

	/**
	 * A reactive health contributor entry.
	 *
	 * @param name the name of the contributor
	 * @param contributor the contributor
	 */
	record Entry(String name, ReactiveHealthContributor contributor) {

		public Entry {
			Assert.hasText(name, "'name' must not be empty");
			Assert.notNull(contributor, "'contributor' must not be null");
		}

	}

}
