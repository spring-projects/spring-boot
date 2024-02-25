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

package org.springframework.boot.autoconfigure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.context.annotation.Configurations;
import org.springframework.core.Ordered;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.util.ClassUtils;

/**
 * {@link Configurations} representing auto-configuration {@code @Configuration} classes.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
public class AutoConfigurations extends Configurations implements Ordered {

	private static final AutoConfigurationSorter SORTER = new AutoConfigurationSorter(new SimpleMetadataReaderFactory(),
			null);

	private static final Ordered ORDER = new AutoConfigurationImportSelector();

	/**
     * Constructs a new AutoConfigurations object with the specified collection of classes.
     *
     * @param classes the collection of classes to be used for auto-configurations
     */
    protected AutoConfigurations(Collection<Class<?>> classes) {
		super(classes);
	}

	/**
     * Sorts the given collection of classes based on their priority order.
     * 
     * @param classes the collection of classes to be sorted
     * @return a sorted collection of classes
     */
    @Override
	protected Collection<Class<?>> sort(Collection<Class<?>> classes) {
		List<String> names = classes.stream().map(Class::getName).toList();
		List<String> sorted = SORTER.getInPriorityOrder(names);
		return sorted.stream()
			.map((className) -> ClassUtils.resolveClassName(className, null))
			.collect(Collectors.toCollection(ArrayList::new));
	}

	/**
     * Returns the order of the AutoConfigurations class.
     * 
     * @return the order of the AutoConfigurations class
     */
    @Override
	public int getOrder() {
		return ORDER.getOrder();
	}

	/**
     * Merges the given set of merged classes into a new instance of AutoConfigurations.
     *
     * @param mergedClasses the set of merged classes to be merged
     * @return a new instance of AutoConfigurations with the merged classes
     */
    @Override
	protected AutoConfigurations merge(Set<Class<?>> mergedClasses) {
		return new AutoConfigurations(mergedClasses);
	}

	/**
     * Creates an instance of AutoConfigurations with the specified classes.
     * 
     * @param classes the classes to be used for auto configuration
     * @return an instance of AutoConfigurations
     */
    public static AutoConfigurations of(Class<?>... classes) {
		return new AutoConfigurations(Arrays.asList(classes));
	}

}
