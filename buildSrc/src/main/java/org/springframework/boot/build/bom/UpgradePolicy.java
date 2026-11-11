/*
 * Copyright 2012-present the original author or authors.
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
	SAME_MAJOR_VERSION(DependencyVersion::isSameMajor),

	/**
	 * Patch versions of the current minor version.
	 */
	SAME_MINOR_VERSION(DependencyVersion::isSameMinor);

	private final BiPredicate<DependencyVersion, DependencyVersion> delegate;

	UpgradePolicy(BiPredicate<DependencyVersion, DependencyVersion> delegate) {
		this.delegate = delegate;
	}

	@Override
	public boolean test(DependencyVersion candidate, DependencyVersion current) {
		return this.delegate.test(candidate, current);
	}

	public static UpgradePolicy max(UpgradePolicy one, UpgradePolicy two) {
		if (one == null && two != null) {
			return two;
		}
		else if (one != null && two == null) {
			return one;
		}
		return (one.ordinal() < two.ordinal()) ? one : two;
	}

}
