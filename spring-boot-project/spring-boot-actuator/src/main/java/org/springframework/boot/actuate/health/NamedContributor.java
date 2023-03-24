/*
 * Copyright 2012-2023 the original author or authors.
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
 * A single named health endpoint contributors (either {@link HealthContributor} or
 * {@link ReactiveHealthContributor}).
 *
 * @param <C> the contributor type
 * @author Phillip Webb
 * @since 2.0.0
 * @see NamedContributors
 */
public interface NamedContributor<C> {

	/**
	 * Returns the name of the contributor.
	 * @return the contributor name
	 */
	String getName();

	/**
	 * Returns the contributor instance.
	 * @return the contributor instance
	 */
	C getContributor();

	static <C> NamedContributor<C> of(String name, C contributor) {
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(contributor, "Contributor must not be null");
		return new NamedContributor<>() {

			@Override
			public String getName() {
				return name;
			}

			@Override
			public C getContributor() {
				return contributor;
			}

		};
	}

}
