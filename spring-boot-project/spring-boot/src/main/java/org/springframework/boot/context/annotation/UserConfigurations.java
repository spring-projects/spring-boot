/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

	protected UserConfigurations(Collection<Class<?>> classes) {
		super(classes);
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	@Override
	protected UserConfigurations merge(Set<Class<?>> mergedClasses) {
		return new UserConfigurations(mergedClasses);
	}

	public static UserConfigurations of(Class<?>... classes) {
		return new UserConfigurations(Arrays.asList(classes));
	}

}
