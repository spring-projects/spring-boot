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

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import org.springframework.util.Assert;

/**
 * Default {@link ContributorRegistry} implementation.
 *
 * @param <C> the health contributor type
 * @author Phillip Webb
 * @see DefaultHealthContributorRegistry
 * @see DefaultReactiveHealthContributorRegistry
 */
class DefaultContributorRegistry<C> implements ContributorRegistry<C> {

	private final Function<String, String> nameFactory;

	private final Object monitor = new Object();

	private volatile Map<String, C> contributors;

	DefaultContributorRegistry() {
		this(Collections.emptyMap());
	}

	DefaultContributorRegistry(Map<String, C> contributors) {
		this(contributors, HealthContributorNameFactory.INSTANCE);
	}

	DefaultContributorRegistry(Map<String, C> contributors, Function<String, String> nameFactory) {
		Assert.notNull(contributors, "Contributors must not be null");
		Assert.notNull(nameFactory, "NameFactory must not be null");
		this.nameFactory = nameFactory;
		Map<String, C> namedContributors = new LinkedHashMap<>();
		contributors.forEach((name, contributor) -> namedContributors.put(nameFactory.apply(name), contributor));
		this.contributors = Collections.unmodifiableMap(namedContributors);
	}

	@Override
	public void registerContributor(String name, C contributor) {
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(contributor, "Contributor must not be null");
		String adaptedName = this.nameFactory.apply(name);
		synchronized (this.monitor) {
			Assert.state(!this.contributors.containsKey(adaptedName),
					() -> "A contributor named \"" + adaptedName + "\" has already been registered");
			Map<String, C> contributors = new LinkedHashMap<>(this.contributors);
			contributors.put(adaptedName, contributor);
			this.contributors = Collections.unmodifiableMap(contributors);
		}
	}

	@Override
	public C unregisterContributor(String name) {
		Assert.notNull(name, "Name must not be null");
		String adaptedName = this.nameFactory.apply(name);
		synchronized (this.monitor) {
			C unregistered = this.contributors.get(adaptedName);
			if (unregistered != null) {
				Map<String, C> contributors = new LinkedHashMap<>(this.contributors);
				contributors.remove(adaptedName);
				this.contributors = Collections.unmodifiableMap(contributors);
			}
			return unregistered;
		}
	}

	@Override
	public C getContributor(String name) {
		return this.contributors.get(name);
	}

	@Override
	public Iterator<NamedContributor<C>> iterator() {
		Iterator<Map.Entry<String, C>> iterator = this.contributors.entrySet().iterator();
		return new Iterator<NamedContributor<C>>() {

			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public NamedContributor<C> next() {
				Entry<String, C> entry = iterator.next();
				return NamedContributor.of(entry.getKey(), entry.getValue());
			}

		};
	}

}
