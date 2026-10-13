/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.flyway.autoconfigure;

import org.flywaydb.core.api.configuration.FluentConfiguration;

/**
 * A Flyway customizer which gets replaced with
 * {@link NativeImageResourceProviderCustomizer} when running in a native image.
 *
 * @author Moritz Halbritter
 */
class ResourceProviderCustomizer {

	void customize(FluentConfiguration configuration) {
	}

}
