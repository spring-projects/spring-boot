/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testcontainers.service.connection;

import java.util.function.Supplier;

import org.testcontainers.containers.Container;

import org.springframework.boot.origin.Origin;
import org.springframework.core.annotation.MergedAnnotation;

/**
 * Factory for tests to create a {@link ContainerConnectionSource}.
 *
 * @author Phillip Webb
 */
public final class TestContainerConnectionSource {

	private TestContainerConnectionSource() {
	}

	public static <C extends Container<?>> ContainerConnectionSource<C> create(String beanNameSuffix, Origin origin,
			Class<C> containerType, String containerImageName, MergedAnnotation<ServiceConnection> annotation,
			Supplier<C> containerSupplier) {
		return new ContainerConnectionSource<>(beanNameSuffix, origin, containerType, containerImageName, annotation,
				containerSupplier, null, null);
	}

}
