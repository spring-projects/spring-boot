/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testcontainers.context;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.testcontainers.containers.Container;
import org.testcontainers.lifecycle.Startable;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.container.ContainerImageMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Used by {@link ImportTestcontainersRegistrar} to import {@link Container} fields.
 *
 * @author Phillip Webb
 */
class ContainerFieldsImporter {

	Set<Startable> registerBeanDefinitions(BeanDefinitionRegistry registry, Class<?> definitionClass) {
		Set<Startable> importedContainers = new HashSet<>();
		for (Field field : getContainerFields(definitionClass)) {
			assertValid(field);
			Container<?> container = getContainer(field);
			if (container instanceof Startable startable) {
				importedContainers.add(startable);
			}
			registerBeanDefinition(registry, field, container);
		}
		return importedContainers;
	}

	private List<Field> getContainerFields(Class<?> containersClass) {
		List<Field> containerFields = new ArrayList<>();
		ReflectionUtils.doWithFields(containersClass, containerFields::add, this::isContainerField);
		return List.copyOf(containerFields);
	}

	private boolean isContainerField(Field candidate) {
		return Container.class.isAssignableFrom(candidate.getType());
	}

	private void assertValid(Field field) {
		Assert.state(Modifier.isStatic(field.getModifiers()),
				() -> "Container field '" + field.getName() + "' must be static");
	}

	private Container<?> getContainer(Field field) {
		ReflectionUtils.makeAccessible(field);
		Container<?> container = (Container<?>) ReflectionUtils.getField(field, null);
		Assert.state(container != null, () -> "Container field '" + field.getName() + "' must not have a null value");
		return container;
	}

	private void registerBeanDefinition(BeanDefinitionRegistry registry, Field field, Container<?> container) {
		ContainerImageMetadata containerMetadata = new ContainerImageMetadata(container.getDockerImageName());
		TestcontainerFieldBeanDefinition beanDefinition = new TestcontainerFieldBeanDefinition(field, container);
		containerMetadata.addTo(beanDefinition);
		String beanName = generateBeanName(field);
		registry.registerBeanDefinition(beanName, beanDefinition);
	}

	private String generateBeanName(Field field) {
		return "importTestContainer.%s.%s".formatted(field.getDeclaringClass().getName(), field.getName());
	}

}
