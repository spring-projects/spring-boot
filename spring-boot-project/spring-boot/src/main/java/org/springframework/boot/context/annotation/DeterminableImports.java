/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.context.annotation;

import java.util.Set;

import org.springframework.beans.factory.Aware;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Interface that can be implemented by {@link ImportSelector} and
 * {@link ImportBeanDefinitionRegistrar} implementations when they can determine imports
 * early. The {@link ImportSelector} and {@link ImportBeanDefinitionRegistrar} interfaces
 * are quite flexible which can make it hard to tell exactly what bean definitions they
 * will add. This interface should be used when an implementation consistently results in
 * the same imports, given the same source.
 * <p>
 * Using {@link DeterminableImports} is particularly useful when working with Spring's
 * testing support. It allows for better generation of {@link ApplicationContext} cache
 * keys.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.5.0
 */
@FunctionalInterface
public interface DeterminableImports {

	/**
	 * Return a set of objects that represent the imports. Objects within the returned
	 * {@code Set} must implement a valid {@link Object#hashCode() hashCode} and
	 * {@link Object#equals(Object) equals}.
	 * <p>
	 * Imports from multiple {@link DeterminableImports} instances may be combined by the
	 * caller to create a complete set.
	 * <p>
	 * Unlike {@link ImportSelector} and {@link ImportBeanDefinitionRegistrar} any
	 * {@link Aware} callbacks will not be invoked before this method is called.
	 * @param metadata the source meta-data
	 * @return a key representing the annotations that actually drive the import
	 */
	Set<Object> determineImports(AnnotationMetadata metadata);

}
