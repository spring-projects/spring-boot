/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.flyway;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.migration.JavaMigration;
import org.flywaydb.core.internal.scanner.LocationScannerCache;
import org.flywaydb.core.internal.scanner.ResourceNameCache;
import org.flywaydb.core.internal.scanner.Scanner;

import org.springframework.util.ClassUtils;

/**
 * Registers {@link NativeImageResourceProvider} as a Flyway
 * {@link org.flywaydb.core.api.ResourceProvider}.
 *
 * @author Moritz Halbritter
 * @author Maziz Esa
 */
class NativeImageResourceProviderCustomizer extends ResourceProviderCustomizer {

	@Override
	public void customize(FluentConfiguration configuration) {
		if (configuration.getResourceProvider() == null) {
			final var scanner = getFlyway9OrFallbackTo10ScannerObject(configuration);
			NativeImageResourceProvider resourceProvider = new NativeImageResourceProvider(scanner,
					configuration.getClassLoader(), Arrays.asList(configuration.getLocations()),
					configuration.getEncoding(), configuration.isFailOnMissingLocations());
			configuration.resourceProvider(resourceProvider);
		}
	}

	private static Scanner getFlyway9OrFallbackTo10ScannerObject(FluentConfiguration configuration) {
		Scanner scanner;
		try {
			scanner = getFlyway9Scanner(configuration);
		}
		catch (NoSuchMethodError noSuchMethodError) {
			// happens when scanner is flyway version 10, which the constructor accepts
			// different number of parameters.
			scanner = getFlyway10Scanner(configuration);
		}
		return scanner;
	}

	private static Scanner getFlyway10Scanner(FluentConfiguration configuration) {
		final Constructor<?> scannerConstructor;
		final Scanner scanner;
		try {
			scannerConstructor = ClassUtils.forName("org.flywaydb.core.internal.scanner.Scanner", null)
				.getDeclaredConstructors()[0];
			scanner = (Scanner) scannerConstructor.newInstance(JavaMigration.class, false, new ResourceNameCache(),
					new LocationScannerCache(), configuration);
		}
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| InvocationTargetException ex) {
			throw new RuntimeException(ex);
		}
		return scanner;
	}

	private static Scanner getFlyway9Scanner(FluentConfiguration configuration) {
		Scanner scanner;
		scanner = new Scanner<>(JavaMigration.class, Arrays.asList(configuration.getLocations()),
				configuration.getClassLoader(), configuration.getEncoding(), configuration.isDetectEncoding(), false,
				new ResourceNameCache(), new LocationScannerCache(), configuration.isFailOnMissingLocations());
		return scanner;
	}

}
