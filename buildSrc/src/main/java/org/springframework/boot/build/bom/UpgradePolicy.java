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

package org.springframework.boot.build.bom;

import java.util.function.BiPredicate;

import org.springframework.boot.build.bom.bomr.version.DependencyVersion;

/**
 * Policies used to decide which versions are considered as possible upgrades.
 *
 * @author Andy Wilkinson
 */
public enum UpgradePolicy implements BiPredicate<DependencyVersion, DependencyVersion> {

	/**
	 * Any version.
	 */
	ANY((candidate, current) -> true),

	/**
	 * Minor versions of the current major version.
	 */
	SAME_MAJOR_VERSION((candidate, current) -> candidate.isSameMajor(current)),

	/**
	 * Patch versions of the current minor version.
	 */
	SAME_MINOR_VERSION((candidate, current) -> candidate.isSameMinor(current));

	private final BiPredicate<DependencyVersion, DependencyVersion> delegate;

	/**
     * Constructs a new UpgradePolicy with the specified delegate.
     *
     * @param delegate the BiPredicate used to determine if an upgrade is necessary
     */
    UpgradePolicy(BiPredicate<DependencyVersion, DependencyVersion> delegate) {
		this.delegate = delegate;
	}

	/**
     * Tests if the candidate dependency version is eligible for upgrade based on the current dependency version.
     * 
     * @param candidate The candidate dependency version to be tested.
     * @param current The current dependency version.
     * @return {@code true} if the candidate dependency version is eligible for upgrade, {@code false} otherwise.
     */
    @Override
	public boolean test(DependencyVersion candidate, DependencyVersion current) {
		return this.delegate.test(candidate, current);
	}

}
