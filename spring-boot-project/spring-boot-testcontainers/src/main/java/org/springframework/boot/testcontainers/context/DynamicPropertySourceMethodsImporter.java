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

package org.springframework.boot.testcontainers.context;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
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

	/**
	 * Constructs a new DynamicPropertySourceMethodsImporter with the specified
	 * environment.
	 * @param environment the environment to be used by the importer
	 */
	DynamicPropertySourceMethodsImporter(Environment environment) {
		this.environment = environment;
	}

	/**
	 * Registers dynamic property sources for the given bean definition registry and
	 * definition class.
	 * @param beanDefinitionRegistry the bean definition registry to register the dynamic
	 * property sources with
	 * @param definitionClass the class containing the dynamic property source methods
	 */
	void registerDynamicPropertySources(BeanDefinitionRegistry beanDefinitionRegistry, Class<?> definitionClass) {
		Set<Method> methods = MethodIntrospector.selectMethods(definitionClass, this::isAnnotated);
		if (methods.isEmpty()) {
			return;
		}
		DynamicPropertyRegistry dynamicPropertyRegistry = TestcontainersPropertySource.attach(this.environment,
				beanDefinitionRegistry);
		methods.forEach((method) -> {
			assertValid(method);
			ReflectionUtils.makeAccessible(method);
			ReflectionUtils.invokeMethod(method, null, dynamicPropertyRegistry);
		});
	}

	/**
	 * Checks if the given method is annotated with {@link DynamicPropertySource}.
	 * @param method the method to check
	 * @return {@code true} if the method is annotated with {@link DynamicPropertySource},
	 * {@code false} otherwise
	 */
	private boolean isAnnotated(Method method) {
		return MergedAnnotations.from(method).isPresent(DynamicPropertySource.class);
	}

	/**
	 * Asserts the validity of a given method for use as a dynamic property source.
	 * @param method the method to be validated
	 * @throws IllegalStateException if the method is not static or does not accept a
	 * single DynamicPropertyRegistry argument
	 */
	private void assertValid(Method method) {
		Assert.state(Modifier.isStatic(method.getModifiers()),
				() -> "@DynamicPropertySource method '" + method.getName() + "' must be static");
		Class<?>[] types = method.getParameterTypes();
		Assert.state(types.length == 1 && types[0] == DynamicPropertyRegistry.class,
				() -> "@DynamicPropertySource method '" + method.getName()
						+ "' must accept a single DynamicPropertyRegistry argument");
	}

}
