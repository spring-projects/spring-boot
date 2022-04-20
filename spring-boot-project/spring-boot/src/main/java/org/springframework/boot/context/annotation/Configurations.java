/*
 * Copyright 2012-2022 the original author or authors.
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

	private final Set<Class<?>> classes;

	protected Configurations(Collection<Class<?>> classes) {
		Assert.notNull(classes, "Classes must not be null");
		Collection<Class<?>> sorted = sort(classes);
		this.classes = Collections.unmodifiableSet(new LinkedHashSet<>(sorted));
	}

	/**
	 * Sort configuration classes into the order that they should be applied.
	 * @param classes the classes to sort
	 * @return a sorted set of classes
	 */
	protected Collection<Class<?>> sort(Collection<Class<?>> classes) {
		return classes;
	}

	protected final Set<Class<?>> getClasses() {
		return this.classes;
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
		return merge(mergedClasses);
	}

	/**
	 * Merge configurations.
	 * @param mergedClasses the merged classes
	 * @return a new configurations instance (must be of the same type as this instance)
	 */
	protected abstract Configurations merge(Set<Class<?>> mergedClasses);

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
		List<Configurations> ordered = new ArrayList<>(configurations);
		ordered.sort(COMPARATOR);
		List<Configurations> collated = collate(ordered);
		LinkedHashSet<Class<?>> classes = collated.stream().flatMap(Configurations::streamClasses)
				.collect(Collectors.toCollection(LinkedHashSet::new));
		return ClassUtils.toClassArray(classes);
	}

	private static Stream<Class<?>> streamClasses(Configurations configurations) {
		return configurations.getClasses().stream();
	}

	private static List<Configurations> collate(List<Configurations> orderedConfigurations) {
		LinkedList<Configurations> collated = new LinkedList<>();
		for (Configurations item : orderedConfigurations) {
			if (collated.isEmpty() || collated.getLast().getClass() != item.getClass()) {
				collated.add(item);
			}
			else {
				collated.set(collated.size() - 1, collated.getLast().merge(item));
			}
		}
		return collated;
	}

}
