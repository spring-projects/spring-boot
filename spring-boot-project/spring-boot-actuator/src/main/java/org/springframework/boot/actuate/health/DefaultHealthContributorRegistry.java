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

import java.util.Map;
import java.util.function.Function;

/**
 * Default {@link HealthContributorRegistry} implementation.
 *
 * @author Phillip Webb
 * @since 2.2.0
 */
public class DefaultHealthContributorRegistry extends DefaultContributorRegistry<HealthContributor>
		implements HealthContributorRegistry {

	/**
	 * Constructs a new DefaultHealthContributorRegistry.
	 */
	public DefaultHealthContributorRegistry() {
	}

	/**
	 * Constructs a new DefaultHealthContributorRegistry with the specified contributors.
	 * @param contributors a map of contributors where the key is the contributor name and
	 * the value is the HealthContributor instance
	 */
	public DefaultHealthContributorRegistry(Map<String, HealthContributor> contributors) {
		super(contributors);
	}

	/**
	 * Constructs a new DefaultHealthContributorRegistry with the specified contributors
	 * and name factory.
	 * @param contributors the map of contributors to be registered in the registry
	 * @param nameFactory the function used to generate names for the contributors
	 */
	public DefaultHealthContributorRegistry(Map<String, HealthContributor> contributors,
			Function<String, String> nameFactory) {
		super(contributors, nameFactory);
	}

}
