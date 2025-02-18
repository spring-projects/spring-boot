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
import java.util.function.Supplier;

import org.testcontainers.lifecycle.Startable;

import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Used by {@link ImportTestcontainersRegistrar} to import
 * {@link DynamicPropertySource @DynamicPropertySource} through a
 * {@link DynamicPropertyRegistrar}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class DynamicPropertySourceMethodsImporter {

	void registerDynamicPropertySources(BeanDefinitionRegistry beanDefinitionRegistry, Class<?> definitionClass,
			Set<Startable> importedContainers) {
		Set<Method> methods = MethodIntrospector.selectMethods(definitionClass, this::isAnnotated);
		if (methods.isEmpty()) {
			return;
		}
		methods.forEach((method) -> assertValid(method));
		RootBeanDefinition registrarDefinition = new RootBeanDefinition();
		registrarDefinition.setBeanClass(DynamicPropertySourcePropertyRegistrar.class);
		ConstructorArgumentValues arguments = new ConstructorArgumentValues();
		arguments.addGenericArgumentValue(methods);
		arguments.addGenericArgumentValue(importedContainers);
		registrarDefinition.setConstructorArgumentValues(arguments);
		beanDefinitionRegistry.registerBeanDefinition(definitionClass.getName() + ".dynamicPropertyRegistrar",
				registrarDefinition);
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

	static class DynamicPropertySourcePropertyRegistrar implements DynamicPropertyRegistrar {

		private final Set<Method> methods;

		private final Set<Startable> containers;

		DynamicPropertySourcePropertyRegistrar(Set<Method> methods, Set<Startable> containers) {
			this.methods = methods;
			this.containers = containers;
		}

		@Override
		public void accept(DynamicPropertyRegistry registry) {
			DynamicPropertyRegistry containersBackedRegistry = new ContainersBackedDynamicPropertyRegistry(registry,
					this.containers);
			this.methods.forEach((method) -> {
				ReflectionUtils.makeAccessible(method);
				ReflectionUtils.invokeMethod(method, null, containersBackedRegistry);
			});
		}

	}

	static class ContainersBackedDynamicPropertyRegistry implements DynamicPropertyRegistry {

		private final DynamicPropertyRegistry delegate;

		private final Set<Startable> containers;

		ContainersBackedDynamicPropertyRegistry(DynamicPropertyRegistry delegate, Set<Startable> containers) {
			this.delegate = delegate;
			this.containers = containers;
		}

		@Override
		public void add(String name, Supplier<Object> valueSupplier) {
			this.delegate.add(name, () -> {
				startContainers();
				return valueSupplier.get();
			});
		}

		private void startContainers() {
			this.containers.forEach(Startable::start);
		}

	}

}
