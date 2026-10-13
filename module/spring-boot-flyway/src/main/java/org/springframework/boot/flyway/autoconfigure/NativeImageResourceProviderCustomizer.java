/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.flyway.autoconfigure;

import java.util.Arrays;

import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.migration.JavaMigration;
import org.flywaydb.core.internal.scanner.Scanner;

/**
 * Registers {@link NativeImageResourceProvider} as a Flyway
 * {@link org.flywaydb.core.api.ResourceProvider}.
 *
 * @author Moritz Halbritter
 */
class NativeImageResourceProviderCustomizer extends ResourceProviderCustomizer {

	@Override
	public void customize(FluentConfiguration configuration) {
		if (configuration.getResourceProvider() == null) {
			Scanner<JavaMigration> scanner = new Scanner<>(JavaMigration.class, configuration,
					configuration.getLocations());
			NativeImageResourceProvider resourceProvider = new NativeImageResourceProvider(scanner,
					configuration.getClassLoader(), Arrays.asList(configuration.getLocations()),
					configuration.getEncoding(), configuration.isFailOnMissingLocations());
			configuration.resourceProvider(resourceProvider);
		}
	}

}
