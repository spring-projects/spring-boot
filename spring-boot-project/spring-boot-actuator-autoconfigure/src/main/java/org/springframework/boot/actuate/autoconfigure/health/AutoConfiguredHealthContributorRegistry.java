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

package org.springframework.boot.actuate.autoconfigure.health;

import java.util.Collection;
import java.util.Map;

import org.springframework.boot.actuate.health.DefaultHealthContributorRegistry;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.util.Assert;

/**
 * An auto-configured {@link HealthContributorRegistry} that ensures registered indicators
 * do not clash with groups names.
 *
 * @author Phillip Webb
 */
class AutoConfiguredHealthContributorRegistry extends DefaultHealthContributorRegistry {

	private final Collection<String> groupNames;

	/**
     * Constructs a new AutoConfiguredHealthContributorRegistry with the specified contributors and group names.
     * 
     * @param contributors a map of contributors, where the key is the contributor name and the value is the contributor instance
     * @param groupNames a collection of group names
     */
    AutoConfiguredHealthContributorRegistry(Map<String, HealthContributor> contributors,
			Collection<String> groupNames) {
		super(contributors);
		this.groupNames = groupNames;
		contributors.keySet().forEach(this::assertDoesNotClashWithGroup);
	}

	/**
     * Registers a health contributor with the given name.
     * 
     * @param name the name of the contributor
     * @param contributor the health contributor to be registered
     * @throws IllegalArgumentException if the name clashes with an existing contributor in the group
     */
    @Override
	public void registerContributor(String name, HealthContributor contributor) {
		assertDoesNotClashWithGroup(name);
		super.registerContributor(name, contributor);
	}

	/**
     * Asserts that the given name does not clash with any group names in the AutoConfiguredHealthContributorRegistry.
     * 
     * @param name the name to check for clashes
     * @throws IllegalStateException if the name clashes with any group name
     */
    private void assertDoesNotClashWithGroup(String name) {
		Assert.state(!this.groupNames.contains(name),
				() -> "HealthContributor with name \"" + name + "\" clashes with group");
	}

}
