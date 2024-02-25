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

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;

/**
 * {@link Configurations} representing user-defined {@code @Configuration} classes (i.e.
 * those defined in classes usually written by the user).
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
public class UserConfigurations extends Configurations implements PriorityOrdered {

	/**
	 * Constructs a new UserConfigurations object with the specified collection of
	 * classes.
	 * @param classes the collection of classes to be used for user configurations
	 */
	protected UserConfigurations(Collection<Class<?>> classes) {
		super(classes);
	}

	/**
	 * Returns the order of this configuration bean. The order is set to the lowest
	 * precedence.
	 * @return the order of this configuration bean
	 */
	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	/**
	 * Merges the given set of classes into a new instance of UserConfigurations.
	 * @param mergedClasses the set of classes to be merged
	 * @return a new instance of UserConfigurations with the merged classes
	 */
	@Override
	protected UserConfigurations merge(Set<Class<?>> mergedClasses) {
		return new UserConfigurations(mergedClasses);
	}

	/**
	 * Creates a new UserConfigurations object with the specified classes.
	 * @param classes the classes to be included in the UserConfigurations object
	 * @return a new UserConfigurations object
	 */
	public static UserConfigurations of(Class<?>... classes) {
		return new UserConfigurations(Arrays.asList(classes));
	}

}
