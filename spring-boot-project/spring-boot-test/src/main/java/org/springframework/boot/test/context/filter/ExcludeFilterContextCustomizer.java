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

package org.springframework.boot.test.context.filter;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * {@link ContextCustomizer} to add the {@link TestTypeExcludeFilter} to the
 * {@link ApplicationContext}.
 *
 * @author Phillip Webb
 */
class ExcludeFilterContextCustomizer implements ContextCustomizer {

	/**
     * Customize the application context by registering the TestTypeExcludeFilter with the bean factory.
     * 
     * @param context the configurable application context
     * @param mergedContextConfiguration the merged context configuration
     */
    @Override
	public void customizeContext(ConfigurableApplicationContext context,
			MergedContextConfiguration mergedContextConfiguration) {
		TestTypeExcludeFilter.registerWith(context.getBeanFactory());
	}

	/**
     * Compares this object with the specified object for equality.
     * 
     * @param obj the object to compare with
     * @return {@code true} if the specified object is of the same class as this object, {@code false} otherwise
     */
    @Override
	public boolean equals(Object obj) {
		return (obj != null) && (getClass() == obj.getClass());
	}

	/**
     * Returns a hash code value for the object. This method overrides the default implementation of the hashCode() method.
     *
     * @return the hash code value for this object
     */
    @Override
	public int hashCode() {
		return getClass().hashCode();
	}

}
