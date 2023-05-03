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

package org.springframework.boot.testcontainers.context;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

import org.springframework.boot.testcontainers.properties.TestcontainersPropertySource;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Used by {@link ImportTestcontainersRegistrar} to import
 * {@link DynamicPropertySource @DynamicPropertySource} methods.
 *
 * @author Phillip Webb
 */
class DynamicPropertySourceMethodsImporter {

	private final Environment environment;

	DynamicPropertySourceMethodsImporter(Environment environment) {
		this.environment = environment;
	}

	void registerDynamicPropertySources(Class<?> definitionClass) {
		Set<Method> methods = MethodIntrospector.selectMethods(definitionClass, this::isAnnotated);
		if (methods.isEmpty()) {
			return;
		}
		DynamicPropertyRegistry registry = TestcontainersPropertySource.attach(this.environment);
		methods.forEach((method) -> {
			assertValid(method);
			ReflectionUtils.makeAccessible(method);
			ReflectionUtils.invokeMethod(method, null, registry);
		});
	}

	private boolean isAnnotated(Method method) {
		return MergedAnnotations.from(method).isPresent(DynamicPropertySource.class);
	}

	private void assertValid(Method method) {
		Assert.state(Modifier.isStatic(method.getModifiers()),
				() -> "@DynamicPropertySource method '" + method.getName() + "' must be static");
		Class<?>[] types = method.getParameterTypes();
		Assert.state(types.length == 1 && types[0] == DynamicPropertyRegistry.class,
				() -> "@DynamicPropertySource method '" + method.getName()
						+ "' must accept a single DynamicPropertyRegistry argument");
	}

}
