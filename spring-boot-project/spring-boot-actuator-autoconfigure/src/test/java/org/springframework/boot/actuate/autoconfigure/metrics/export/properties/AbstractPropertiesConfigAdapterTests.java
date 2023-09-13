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

package org.springframework.boot.actuate.autoconfigure.metrics.export.properties;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import io.micrometer.core.instrument.config.validate.Validated;
import org.junit.jupiter.api.Test;

import org.springframework.core.annotation.AnnotatedElementUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for testing properties config adapters.
 *
 * @param <P> the properties used by the adapter
 * @param <A> the adapter under test
 * @author Andy Wilkinson
 * @author Mirko Sobeck
 */
public abstract class AbstractPropertiesConfigAdapterTests<P, A extends PropertiesConfigAdapter<P>> {

	private final Class<? extends A> adapter;

	protected AbstractPropertiesConfigAdapterTests(Class<? extends A> adapter) {
		this.adapter = adapter;
	}

	@Test
	protected void adapterOverridesAllConfigMethods() {
		adapterOverridesAllConfigMethodsExcept();
	}

	protected final void adapterOverridesAllConfigMethodsExcept(String... nonConfigMethods) {
		Class<?> config = findImplementedConfig();
		Set<String> expectedConfigMethodNames = Arrays.stream(config.getDeclaredMethods())
			.filter(Method::isDefault)
			.filter(this::hasNoParameters)
			.filter(this::isNotValidationMethod)
			.filter(this::isNotDeprecated)
			.map(Method::getName)
			.collect(Collectors.toCollection(TreeSet::new));
		expectedConfigMethodNames.removeAll(Arrays.asList(nonConfigMethods));
		Set<String> actualConfigMethodNames = new TreeSet<>();
		Class<?> currentClass = this.adapter;
		while (!Object.class.equals(currentClass)) {
			actualConfigMethodNames.addAll(Arrays.stream(currentClass.getDeclaredMethods())
				.map(Method::getName)
				.filter(expectedConfigMethodNames::contains)
				.toList());
			currentClass = currentClass.getSuperclass();
		}
		assertThat(actualConfigMethodNames).containsExactlyInAnyOrderElementsOf(expectedConfigMethodNames);
	}

	private Class<?> findImplementedConfig() {
		Class<?>[] interfaces = this.adapter.getInterfaces();
		if (interfaces.length == 1) {
			return interfaces[0];
		}
		throw new IllegalStateException(this.adapter + " is not a config implementation");
	}

	private boolean isNotDeprecated(Method method) {
		return !AnnotatedElementUtils.hasAnnotation(method, Deprecated.class);
	}

	private boolean hasNoParameters(Method method) {
		return method.getParameterCount() == 0;
	}

	private boolean isNotValidationMethod(Method method) {
		return !Validated.class.equals(method.getReturnType());
	}

}
