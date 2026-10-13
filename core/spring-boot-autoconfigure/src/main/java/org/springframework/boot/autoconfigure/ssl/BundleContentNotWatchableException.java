/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.ssl;

/**
 * Thrown when a bundle content location is not watchable.
 *
 * @author Moritz Halbritter
 */
class BundleContentNotWatchableException extends RuntimeException {

	private final BundleContentProperty property;

	BundleContentNotWatchableException(BundleContentProperty property) {
		super("The content of '%s' is not watchable. Only 'file:' resources are watchable, but '%s' has been set"
			.formatted(property.name(), property.value()));
		this.property = property;
	}

	private BundleContentNotWatchableException(String bundleName, BundleContentProperty property, Throwable cause) {
		super("The content of '%s' from bundle '%s' is not watchable'. Only 'file:' resources are watchable, but '%s' has been set"
			.formatted(property.name(), bundleName, property.value()), cause);
		this.property = property;
	}

	BundleContentNotWatchableException withBundleName(String bundleName) {
		return new BundleContentNotWatchableException(bundleName, this.property, this);
	}

}
