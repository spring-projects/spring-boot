/*
 * Copyright 2012-2024 the original author or authors.
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
