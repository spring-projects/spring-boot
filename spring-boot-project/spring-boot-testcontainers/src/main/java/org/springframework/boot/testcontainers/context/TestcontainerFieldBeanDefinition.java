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

import org.testcontainers.containers.Container;

import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.testcontainers.beans.TestcontainerBeanDefinition;
import org.springframework.core.annotation.MergedAnnotations;

/**
 * {@link RootBeanDefinition} used for testcontainer bean definitions.
 * This bean definition links a Testcontainers {@link Container} instance to a field and its 
 * associated annotations.
 *
 * @author Phillip Webb
 */
class TestcontainerFieldBeanDefinition extends RootBeanDefinition implements TestcontainerBeanDefinition {

	private final Container<?> container;

	private final MergedAnnotations annotations;

	/**
     * Create a new {@link TestcontainerFieldBeanDefinition} instance.
     *
     * @param field the field associated with the Testcontainer
     * @param container the container instance associated with the field
     * @throws NullPointerException if field or container is null
     */
	TestcontainerFieldBeanDefinition(Field field, Container<?> container) {
		// Ensure non-null values for the field and container
        this.container = Objects.requireNonNull(container, "Container must not be null");
        this.annotations = MergedAnnotations.from(Objects.requireNonNull(field, "Field must not be null"));
        
        // Set the bean class to the container's class
        this.setBeanClass(container.getClass());
        
        // Provide the container as the instance supplier
        setInstanceSupplier(() -> container);
        
        // Set the role as infrastructure
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
