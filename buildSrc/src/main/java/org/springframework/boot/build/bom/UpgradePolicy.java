/*
 * Copyright 2012-2020 the original author or authors.
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
	 * All versions more recent than the current version will be suggested as possible
	 * upgrades.
	 */
	ANY((candidate, current) -> current.compareTo(candidate) < 0),

	/**
	 * New minor versions of the current major version will be suggested as possible
	 * upgrades. For example, if the current version is 1.2.3, all 1.x.y versions after
	 * 1.2.3 will be suggested. 2.x versions will not be offered.
	 */
	SAME_MAJOR_VERSION(DependencyVersion::isSameMajorAndNewerThan),

	/**
	 * New patch versions of the current minor version will be offered as possible
	 * upgrades. For example, if the current version is 1.2.3, all 1.2.x versions after
	 * 1.2.3 will be suggested. 1.x versions will not be offered.
	 */
	SAME_MINOR_VERSION(DependencyVersion::isSameMinorAndNewerThan);

	private final BiPredicate<DependencyVersion, DependencyVersion> delegate;

	UpgradePolicy(BiPredicate<DependencyVersion, DependencyVersion> delegate) {
		this.delegate = delegate;
	}

	@Override
	public boolean test(DependencyVersion candidate, DependencyVersion current) {
		return this.delegate.test(candidate, current);
	}

}
