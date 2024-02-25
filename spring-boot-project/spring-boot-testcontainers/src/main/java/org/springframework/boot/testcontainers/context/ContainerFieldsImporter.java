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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.testcontainers.containers.Container;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Used by {@link ImportTestcontainersRegistrar} to import {@link Container} fields.
 *
 * @author Phillip Webb
 */
class ContainerFieldsImporter {

	/**
	 * Registers bean definitions for the given definition class in the provided bean
	 * definition registry.
	 * @param registry the bean definition registry to register the bean definitions with
	 * @param definitionClass the class containing the container fields to register as
	 * bean definitions
	 */
	void registerBeanDefinitions(BeanDefinitionRegistry registry, Class<?> definitionClass) {
		for (Field field : getContainerFields(definitionClass)) {
			assertValid(field);
			Container<?> container = getContainer(field);
			registerBeanDefinition(registry, field, container);
		}
	}

	/**
	 * Retrieves a list of container fields from the specified class.
	 * @param containersClass the class to retrieve container fields from
	 * @return a list of container fields found in the class
	 */
	private List<Field> getContainerFields(Class<?> containersClass) {
		List<Field> containerFields = new ArrayList<>();
		ReflectionUtils.doWithFields(containersClass, containerFields::add, this::isContainerField);
		return List.copyOf(containerFields);
	}

	/**
	 * Checks if the given field is a container field.
	 * @param candidate the field to be checked
	 * @return true if the field is a container field, false otherwise
	 */
	private boolean isContainerField(Field candidate) {
		return Container.class.isAssignableFrom(candidate.getType());
	}

	/**
	 * Asserts that the given field is valid.
	 * @param field the field to be validated
	 * @throws IllegalStateException if the field is not static
	 */
	private void assertValid(Field field) {
		Assert.state(Modifier.isStatic(field.getModifiers()),
				() -> "Container field '" + field.getName() + "' must be static");
	}

	/**
	 * Retrieves the container object from the given field.
	 * @param field the field from which to retrieve the container object
	 * @return the container object
	 * @throws IllegalStateException if the container field has a null value
	 */
	private Container<?> getContainer(Field field) {
		ReflectionUtils.makeAccessible(field);
		Container<?> container = (Container<?>) ReflectionUtils.getField(field, null);
		Assert.state(container != null, () -> "Container field '" + field.getName() + "' must not have a null value");
		return container;
	}

	/**
	 * Registers a bean definition in the given bean definition registry for a field in a
	 * container.
	 * @param registry the bean definition registry to register the bean definition with
	 * @param field the field for which the bean definition is being registered
	 * @param container the container containing the field
	 */
	private void registerBeanDefinition(BeanDefinitionRegistry registry, Field field, Container<?> container) {
		TestcontainerFieldBeanDefinition beanDefinition = new TestcontainerFieldBeanDefinition(field, container);
		String beanName = generateBeanName(field);
		registry.registerBeanDefinition(beanName, beanDefinition);
	}

	/**
	 * Generates a bean name for the given field.
	 * @param field the field for which the bean name is generated
	 * @return the generated bean name
	 */
	private String generateBeanName(Field field) {
		return "importTestContainer.%s.%s".formatted(field.getDeclaringClass().getName(), field.getName());
	}

}
