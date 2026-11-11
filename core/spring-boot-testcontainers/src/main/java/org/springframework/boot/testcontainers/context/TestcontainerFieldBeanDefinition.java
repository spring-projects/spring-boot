/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testcontainers.context;

import java.lang.reflect.Field;

import org.testcontainers.containers.Container;

import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.testcontainers.beans.TestcontainerBeanDefinition;
import org.springframework.core.annotation.MergedAnnotations;

/**
 * {@link RootBeanDefinition} used for testcontainer bean definitions.
 *
 * @author Phillip Webb
 */
class TestcontainerFieldBeanDefinition extends RootBeanDefinition implements TestcontainerBeanDefinition {

	private final Container<?> container;

	private final MergedAnnotations annotations;

	TestcontainerFieldBeanDefinition(Field field, Container<?> container) {
		this.container = container;
		this.annotations = MergedAnnotations.from(field);
		this.setBeanClass(container.getClass());
		setInstanceSupplier(() -> container);
		setRole(ROLE_INFRASTRUCTURE);
	}

	@Override
	public String getContainerImageName() {
		return this.container.getDockerImageName();
	}

	@Override
	public MergedAnnotations getAnnotations() {
		return this.annotations;
	}

}
