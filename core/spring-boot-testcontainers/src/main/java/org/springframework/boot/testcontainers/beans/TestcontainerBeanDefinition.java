/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testcontainers.beans;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.annotation.MergedAnnotations;

/**
 * Extended {@link org.springframework.beans.factory.config.BeanDefinition} interface used
 * to register testcontainer beans.
 *
 * @author Phillip Webb
 * @since 3.1.0
 */
public interface TestcontainerBeanDefinition extends BeanDefinition {

	/**
	 * Return the container image name or {@code null} if the image name is not yet known.
	 * @return the container image name
	 */
	@Nullable String getContainerImageName();

	/**
	 * Return any annotations declared alongside the container.
	 * @return annotations declared with the container
	 */
	MergedAnnotations getAnnotations();

}
