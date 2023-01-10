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

package org.springframework.boot.actuate.autoconfigure.metrics.export;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.PropertiesConfigAdapter;

/**
 * Utility to test that all Micrometer config values are exposed via properties and
 * settable using the corresponding {@link PropertiesConfigAdapter} implementation.
 *
 * @author Mirko Sobeck
 */
public final class TestConfigsToPropertiesExposure {

	private TestConfigsToPropertiesExposure() {

	}

	private static final List<Class<?>> classesForPropertiesList = Arrays.asList(Boolean.class, Byte.class,
			Character.class, Short.class, Integer.class, Long.class, Double.class, Float.class, String.class,
			Duration.class);

	/**
	 * Assertion to test if default methods of a given config are overridden by the
	 * adapter which implements it. This can be an indicator for micrometer config fields,
	 * that have been forgotten to expose via spring properties. Not overridden default
	 * methods in adapters are the most common cause of forgotten field exposure, because
	 * they do not for force an override.
	 * @param config micrometer config
	 * @param adapter adapter for properties {@link PropertiesConfigAdapter}
	 * @param excludedConfigMethods config methods that should be excluded for the
	 * assertion
	 */
	public static void assertThatAllConfigDefaultMethodsAreOverriddenByAdapter(Class<?> config,
			Class<? extends PropertiesConfigAdapter<?>> adapter, String... excludedConfigMethods) {
		List<String> configDefaultMethodNames = Arrays.stream(config.getDeclaredMethods())
				.filter((method) -> method.isDefault() && isSettableUsingProperties(method.getReturnType()))
				.map(Method::getName).collect(Collectors.toList());

		configDefaultMethodNames.removeAll(Arrays.stream(excludedConfigMethods).collect(Collectors.toList()));
		List<String> notOverriddenDefaultMethods = new ArrayList<>(configDefaultMethodNames);

		Class<?> currentClass = adapter;
		// loop through adapter class and superclasses
		// to find not overridden config methods
		while (!Object.class.equals(currentClass)) {
			List<String> overriddenClassDefaultMethods = Arrays.stream(currentClass.getDeclaredMethods())
					.map(Method::getName).filter(configDefaultMethodNames::contains).collect(Collectors.toList());

			notOverriddenDefaultMethods.removeAll(overriddenClassDefaultMethods);
			currentClass = currentClass.getSuperclass();
		}

		if (notOverriddenDefaultMethods.size() >= 1) {
			Assert.fail(
					"Found config default methods that are not overridden by the related PropertiesConfigAdapter: \n"
							+ notOverriddenDefaultMethods + "\n"
							+ "This could be an indicator for not exposed properties fields.\n"
							+ "Please check if the fields are meant to be exposed and if not, "
							+ "exclude them from this test by providing them to the method.");
		}
	}

	/**
	 * Guess if a class can be set using properties. This will only catch the basic use
	 * cases regarding the micrometer configs to filter out methods that are not likely to
	 * be designed to be set via properties. <pre>
	 *     isSettableUsingProperties(String.class) = true
	 *     isSettableUsingProperties(boolean.class) = true
	 *     isSettableUsingProperties(Object.class) = false
	 * </pre>
	 * @param clazz Class
	 * @return is likely to be settable using properties
	 */
	private static boolean isSettableUsingProperties(Class<?> clazz) {
		if (Void.TYPE.equals(clazz)) {
			return false;
		}

		if (clazz.isPrimitive()) {
			return true;
		}

		return classesForPropertiesList.contains(clazz);
	}

}
