/*
 * Copyright 2012-2025 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * A set of {@link Configuration @Configuration} classes that can be registered in
 * {@link ApplicationContext}. Classes can be returned from one or more
 * {@link Configurations} instances by using {@link #getClasses(Configurations[])}. The
 * resulting array follows the ordering rules usually applied by the
 * {@link ApplicationContext} and/or custom {@link ImportSelector} implementations.
 * <p>
 * This class is primarily intended for use with tests that need to specify configuration
 * classes but can't use {@link SpringRunner}.
 * <p>
 * Implementations of this class should be annotated with {@link Order @Order} or
 * implement {@link Ordered}.
 *
 * @author Phillip Webb
 * @since 2.0.0
 * @see UserConfigurations
 */
public abstract class Configurations {

	private static final Comparator<Object> COMPARATOR = OrderComparator.INSTANCE
		.thenComparing((other) -> other.getClass().getName());

	private final UnaryOperator<Collection<Class<?>>> sorter;

	private final Set<Class<?>> classes;

	private final Function<Class<?>, String> beanNameGenerator;

	/**
	 * Create a new {@link Configurations} instance.
	 * @param classes the configuration classes
	 */
	protected Configurations(Collection<Class<?>> classes) {
		Assert.notNull(classes, "'classes' must not be null");
		Collection<Class<?>> sorted = sort(classes);
		this.sorter = null;
		this.classes = Collections.unmodifiableSet(new LinkedHashSet<>(sorted));
		this.beanNameGenerator = null;
	}

	/**
	 * Create a new {@link Configurations} instance.
	 * @param sorter a {@link UnaryOperator} used to sort the configurations
	 * @param classes the configuration classes
	 * @param beanNameGenerator an optional function used to generate the bean name
	 * @since 3.4.0
	 */
	protected Configurations(UnaryOperator<Collection<Class<?>>> sorter, Collection<Class<?>> classes,
			Function<Class<?>, String> beanNameGenerator) {
		Assert.notNull(classes, "'classes' must not be null");
		this.sorter = (sorter != null) ? sorter : UnaryOperator.identity();
		Collection<Class<?>> sorted = this.sorter.apply(classes);
		this.classes = Collections.unmodifiableSet(new LinkedHashSet<>(sorted));
		this.beanNameGenerator = beanNameGenerator;
	}

	protected final Set<Class<?>> getClasses() {
		return this.classes;
	}

	/**
	 * Sort configuration classes into the order that they should be applied.
	 * @param classes the classes to sort
	 * @return a sorted set of classes
	 * @deprecated since 3.4.0 for removal in 3.6.0 in favor of
	 * {@link #Configurations(UnaryOperator, Collection, Function)}
	 */
	@Deprecated(since = "3.4.0", forRemoval = true)
	protected Collection<Class<?>> sort(Collection<Class<?>> classes) {
		return classes;
	}

	/**
	 * Merge configurations from another source of the same type.
	 * @param other the other {@link Configurations} (must be of the same type as this
	 * instance)
	 * @return a new configurations instance (must be of the same type as this instance)
	 */
	protected Configurations merge(Configurations other) {
		Set<Class<?>> mergedClasses = new LinkedHashSet<>(getClasses());
		mergedClasses.addAll(other.getClasses());
		if (this.sorter != null) {
			mergedClasses = new LinkedHashSet<>(this.sorter.apply(mergedClasses));
		}
		return merge(mergedClasses);
	}

	/**
	 * Merge configurations.
	 * @param mergedClasses the merged classes
	 * @return a new configurations instance (must be of the same type as this instance)
	 */
	protected abstract Configurations merge(Set<Class<?>> mergedClasses);

	/**
	 * Return the bean name that should be used for the given configuration class or
	 * {@code null} to use the default name.
	 * @param beanClass the bean class
	 * @return the bean name
	 * @since 3.4.0
	 */
	public String getBeanName(Class<?> beanClass) {
		return (this.beanNameGenerator != null) ? this.beanNameGenerator.apply(beanClass) : null;
	}

	/**
	 * Return the classes from all the specified configurations in the order that they
	 * would be registered.
	 * @param configurations the source configuration
	 * @return configuration classes in registration order
	 */
	public static Class<?>[] getClasses(Configurations... configurations) {
		return getClasses(Arrays.asList(configurations));
	}

	/**
	 * Return the classes from all the specified configurations in the order that they
	 * would be registered.
	 * @param configurations the source configuration
	 * @return configuration classes in registration order
	 */
	public static Class<?>[] getClasses(Collection<Configurations> configurations) {
		List<Configurations> collated = collate(configurations);
		LinkedHashSet<Class<?>> classes = collated.stream()
			.flatMap(Configurations::streamClasses)
			.collect(Collectors.toCollection(LinkedHashSet::new));
		return ClassUtils.toClassArray(classes);
	}

	/**
	 * Collate the given configuration by sorting and merging them.
	 * @param configurations the source configuration
	 * @return the collated configurations
	 * @since 3.4.0
	 */
	public static List<Configurations> collate(Collection<Configurations> configurations) {
		LinkedList<Configurations> collated = new LinkedList<>();
		for (Configurations configuration : sortConfigurations(configurations)) {
			if (collated.isEmpty() || collated.getLast().getClass() != configuration.getClass()) {
				collated.add(configuration);
			}
			else {
				collated.set(collated.size() - 1, collated.getLast().merge(configuration));
			}
		}
		return collated;
	}

	private static List<Configurations> sortConfigurations(Collection<Configurations> configurations) {
		List<Configurations> sorted = new ArrayList<>(configurations);
		sorted.sort(COMPARATOR);
		return sorted;
	}

	private static Stream<Class<?>> streamClasses(Configurations configurations) {
		return configurations.getClasses().stream();
	}

}
