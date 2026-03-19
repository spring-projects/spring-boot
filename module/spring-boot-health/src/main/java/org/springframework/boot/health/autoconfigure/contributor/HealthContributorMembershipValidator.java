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

package org.springframework.boot.health.autoconfigure.contributor;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.diagnostics.FailureAnalyzedException;
import org.springframework.boot.health.contributor.HealthContributors;
import org.springframework.boot.health.contributor.ReactiveHealthContributors;
import org.springframework.boot.health.registry.HealthContributorRegistry;
import org.springframework.boot.health.registry.ReactiveHealthContributorRegistry;
import org.springframework.util.CollectionUtils;

/**
 * {@link SmartInitializingSingleton} that validates health membership, throwing a
 * {@link FailureAnalyzedException} if an included or excluded contributor does not exist.
 * This implementation supports the same include/exclude patterns as
 * {@link HealthContributorMembership#byIncludeExclude(Set, Set)}
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 4.1.0
 */
public class HealthContributorMembershipValidator implements SmartInitializingSingleton {

	private final HealthContributorRegistry registry;

	private final @Nullable ReactiveHealthContributorRegistry fallbackRegistry;

	private final String disableValidationProperty;

	private final Consumer<Members> members;

	/**
	 * Create a new {@link HealthContributorMembershipValidator} instance.
	 * @param registry the health registry
	 * @param fallbackRegistry the fallback registry or {@code null}
	 * @param disableValidationProperty the property that can be used to disable
	 * validation
	 * @param members consumer used to provide the members
	 */
	public HealthContributorMembershipValidator(HealthContributorRegistry registry,
			@Nullable ReactiveHealthContributorRegistry fallbackRegistry, String disableValidationProperty,
			Consumer<Members> members) {
		this.registry = registry;
		this.fallbackRegistry = fallbackRegistry;
		this.disableValidationProperty = disableValidationProperty;
		this.members = members;
	}

	@Override
	public void afterSingletonsInstantiated() {
		validateMembers();
	}

	private void validateMembers() {
		this.members.accept(this::validate);
	}

	private void validate(String property, @Nullable Set<String> names) {
		if (CollectionUtils.isEmpty(names)) {
			return;
		}
		for (String name : names) {
			if ("*".equals(name)) {
				return;
			}
			String[] path = name.split("/");
			if (!contributorExists(path)) {
				String description = "Health contributor '%s' defined in '%s' does not exist".formatted(name, property);
				String action = "Update your application to correct the invalid configuration.\nYou can also set '%s' to false to disable the validation."
					.formatted(this.disableValidationProperty);
				throw new FailureAnalyzedException(description, action);
			}
		}
	}

	private boolean contributorExists(String[] path) {
		return contributorExistsInMainRegistry(path) || contributorExistsInFallbackRegistry(path);
	}

	private boolean contributorExistsInMainRegistry(String[] path) {
		return contributorExists(path, this.registry, HealthContributors.class, HealthContributors::getContributor);
	}

	private boolean contributorExistsInFallbackRegistry(String[] path) {
		return contributorExists(path, this.fallbackRegistry, ReactiveHealthContributors.class,
				ReactiveHealthContributors::getContributor);
	}

	@SuppressWarnings("unchecked")
	private <C> boolean contributorExists(String[] path, @Nullable Object registry, Class<C> collectionType,
			BiFunction<C, String, Object> getFromCollection) {
		int pathOffset = 0;
		Object contributor = registry;
		while (pathOffset < path.length) {
			if (contributor == null || !collectionType.isInstance(contributor)) {
				return false;
			}
			contributor = getFromCollection.apply((C) contributor, path[pathOffset]);
			pathOffset++;
		}
		return (contributor != null);
	}

	/**
	 * Callback used to provide member information.
	 */
	@FunctionalInterface
	public interface Members {

		/**
		 * Provide members.
		 * @param property the property providing the members
		 * @param members the members
		 */
		void member(String property, @Nullable Set<String> members);

	}

}
