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

/**
 * A mutable registry of health endpoint contributors (either {@link HealthContributor} or
 * {@link ReactiveHealthContributor}).
 *
 * @param <C> the contributor type
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Vedran Pavic
 * @author Stephane Nicoll
 * @since 2.2.0
 * @see NamedContributors
 */
public interface ContributorRegistry<C> extends NamedContributors<C> {

	/**
	 * Register a contributor with the given {@code name}.
	 * @param name the name of the contributor
	 * @param contributor the contributor to register
	 * @throws IllegalStateException if the contributor cannot be registered with the
	 * given {@code name}.
	 */
	void registerContributor(String name, C contributor);

	/**
	 * Unregister a previously registered contributor.
	 * @param name the name of the contributor to unregister
	 * @return the unregistered indicator, or {@code null} if no indicator was found in
	 * the registry for the given {@code name}.
	 */
	C unregisterContributor(String name);

}
