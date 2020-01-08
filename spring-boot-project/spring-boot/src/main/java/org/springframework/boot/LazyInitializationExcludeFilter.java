/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;

/**
 * Filter that can be used to exclude beans definitions from having their
 * {@link AbstractBeanDefinition#setLazyInit(boolean) lazy-init} set by the
 * {@link LazyInitializationBeanFactoryPostProcessor}.
 * <p>
 * Primarily intended to allow downstream projects to deal with edge-cases in which it is
 * not easy to support lazy-loading (such as in DSLs that dynamically create additional
 * beans). Adding an instance of this filter to the application context can be used for
 * these edge cases.
 * <p>
 * A typical example would be something like this: <pre>
 * &#64;Bean
 * public static LazyInitializationExcludeFilter integrationLazyInitializationExcludeFilter() {
 *   return LazyInitializationExcludeFilter.forBeanTypes(IntegrationFlow.class);
 * }
 * </pre>
 * <p>
 * NOTE: Beans of this type will be instantiated very early in the spring application
 * lifecycle so they should generally be declared static and not have any dependencies.
 *
 * @author Tyler Van Gorder
 * @author Philip Webb
 * @since 2.2.0
 */
@FunctionalInterface
public interface LazyInitializationExcludeFilter {

	/**
	 * Returns {@code true} if the specified bean definition should be excluded from
	 * having {@code lazy-init} automatically set.
	 * @param beanName the bean name
	 * @param beanDefinition the bean definition
	 * @param beanType the bean type
	 * @return {@code true} if {@code lazy-init} should not be automatically set
	 */
	boolean isExcluded(String beanName, BeanDefinition beanDefinition, Class<?> beanType);

	/**
	 * Factory method that creates a filter for the given bean types.
	 * @param types the filtered types
	 * @return a new filter instance
	 */
	static LazyInitializationExcludeFilter forBeanTypes(Class<?>... types) {
		return (beanName, beanDefinition, beanType) -> {
			for (Class<?> type : types) {
				if (type.isAssignableFrom(beanType)) {
					return true;
				}
			}
			return false;
		};
	}

}
