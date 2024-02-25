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

package org.springframework.boot.test.autoconfigure.web.servlet;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * {@link ContextCustomizer} that registers a {@link WebDriverScope} and configures
 * appropriate bean definitions to use it.
 *
 * @author Phillip Webb
 * @see WebDriverScope
 */
class WebDriverContextCustomizer implements ContextCustomizer {

	/**
	 * Customizes the application context by registering the WebDriverScope.
	 * @param context the configurable application context
	 * @param mergedConfig the merged context configuration
	 */
	@Override
	public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
		WebDriverScope.registerWith(context);
	}

	/**
	 * Compares this object with the specified object for equality.
	 * @param obj the object to compare with
	 * @return true if the specified object is equal to this object, false otherwise
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		return obj != null && obj.getClass() == getClass();
	}

	/**
	 * Returns a hash code value for the object. This method overrides the default
	 * implementation of the {@code hashCode()} method.
	 * @return the hash code value for this object
	 */
	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

}
