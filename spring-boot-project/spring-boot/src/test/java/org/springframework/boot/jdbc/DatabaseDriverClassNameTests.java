/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.jdbc;

import java.io.IOException;
import java.sql.Driver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.sql.XADataSource;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.asm.ClassReader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the class names in the {@link DatabaseDriver} enumeration.
 *
 * @author Andy Wilkinson
 */
class DatabaseDriverClassNameTests {

	private static final Set<DatabaseDriver> EXCLUDED_DRIVERS = Collections
			.unmodifiableSet(EnumSet.of(DatabaseDriver.UNKNOWN, DatabaseDriver.DB2_AS400, DatabaseDriver.INFORMIX,
					DatabaseDriver.HANA, DatabaseDriver.TERADATA, DatabaseDriver.REDSHIFT));

	@ParameterizedTest(name = "{0} {2}")
	@MethodSource
	void databaseClassIsOfRequiredType(DatabaseDriver driver, String className, Class<?> requiredType)
			throws Exception {
		assertThat(getInterfaceNames(className.replace('.', '/'))).contains(requiredType.getName().replace('.', '/'));
	}

	private List<String> getInterfaceNames(String className) throws IOException {
		// Use ASM to avoid unwanted side-effects of loading JDBC drivers
		ClassReader classReader = new ClassReader(getClass().getResourceAsStream("/" + className + ".class"));
		List<String> interfaceNames = new ArrayList<>();
		for (String name : classReader.getInterfaces()) {
			interfaceNames.add(name);
			interfaceNames.addAll(getInterfaceNames(name));
		}
		String superName = classReader.getSuperName();
		if (superName != null) {
			interfaceNames.addAll(getInterfaceNames(superName));
		}
		return interfaceNames;
	}

	static Stream<? extends Arguments> databaseClassIsOfRequiredType() {
		return Stream.concat(argumentsForType(Driver.class, DatabaseDriver::getDriverClassName),
				argumentsForType(XADataSource.class,
						(databaseDriver) -> databaseDriver.getXaDataSourceClassName() != null,
						DatabaseDriver::getXaDataSourceClassName));
	}

	private static Stream<? extends Arguments> argumentsForType(Class<?> clazz,
			Function<DatabaseDriver, String> classNameExtractor) {
		return argumentsForType(clazz, (databaseDriver) -> true, classNameExtractor);
	}

	private static Stream<? extends Arguments> argumentsForType(Class<?> clazz, Predicate<DatabaseDriver> predicate,
			Function<DatabaseDriver, String> classNameExtractor) {
		return Stream.of(DatabaseDriver.values()).filter((databaseDriver) -> !EXCLUDED_DRIVERS.contains(databaseDriver))
				.filter(predicate)
				.map((databaseDriver) -> Arguments.of(databaseDriver, classNameExtractor.apply(databaseDriver), clazz));
	}

}
