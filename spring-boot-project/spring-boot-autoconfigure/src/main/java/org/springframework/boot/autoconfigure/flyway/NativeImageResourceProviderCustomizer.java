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

package org.springframework.boot.autoconfigure.flyway;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.migration.JavaMigration;
import org.flywaydb.core.internal.scanner.LocationScannerCache;
import org.flywaydb.core.internal.scanner.ResourceNameCache;
import org.flywaydb.core.internal.scanner.Scanner;
import org.jetbrains.annotations.NotNull;

import org.springframework.util.ClassUtils;

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
			Constructor<?> scannerConstructor;
			Scanner scanner;
			try {
				scannerConstructor = ClassUtils.forName("org.flywaydb.core.internal.scanner.Scanner", null).getDeclaredConstructors()[0];
				if(scannerConstructor.getParameters().length <= 5) {
					scanner = getFlyway10ScannerObject(configuration, scannerConstructor);
				} else {
					scanner = getFlyway9Scanner(configuration, scannerConstructor);
				}
			}
			catch (ClassNotFoundException e) {
				throw new IllegalStateException(e);
			}
			catch (InvocationTargetException | InstantiationException |
				   IllegalAccessException e) {
				throw new RuntimeException(e);
			}
			NativeImageResourceProvider resourceProvider = new NativeImageResourceProvider(scanner,
					configuration.getClassLoader(), Arrays.asList(configuration.getLocations()),
					configuration.getEncoding(), configuration.isFailOnMissingLocations());
			configuration.resourceProvider(resourceProvider);
		}
	}

	private static @NotNull Scanner getFlyway10ScannerObject(FluentConfiguration configuration, Constructor<?> scannerConstructor) throws InstantiationException, IllegalAccessException, InvocationTargetException {
		Scanner scanner;
		scanner = (Scanner) scannerConstructor.newInstance(JavaMigration.class, false, new ResourceNameCache(),
				new LocationScannerCache(), configuration);
		return scanner;
	}

	private static @NotNull Scanner getFlyway9Scanner(FluentConfiguration configuration, Constructor<?> scannerConstructor) throws InstantiationException, IllegalAccessException, InvocationTargetException {
		Scanner scanner;
		scanner = (Scanner) scannerConstructor.newInstance(JavaMigration.class,
				Arrays.asList(configuration.getLocations()), configuration.getClassLoader(),
				configuration.getEncoding(), configuration.isDetectEncoding(), false, new ResourceNameCache(),
				new LocationScannerCache(), configuration.isFailOnMissingLocations());
		return scanner;
	}

}
