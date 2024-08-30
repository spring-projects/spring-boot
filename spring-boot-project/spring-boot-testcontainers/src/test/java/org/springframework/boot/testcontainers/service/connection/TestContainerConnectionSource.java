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
				containerSupplier);
	}

}
