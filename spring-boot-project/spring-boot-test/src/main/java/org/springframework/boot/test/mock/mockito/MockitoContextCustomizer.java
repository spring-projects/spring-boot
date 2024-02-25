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

package org.springframework.boot.test.mock.mockito;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * A {@link ContextCustomizer} to add Mockito support.
 *
 * @author Phillip Webb
 */
class MockitoContextCustomizer implements ContextCustomizer {

	private final Set<Definition> definitions;

	/**
	 * Constructs a new MockitoContextCustomizer with the given set of definitions.
	 * @param definitions the set of definitions to be used by the customizer
	 */
	MockitoContextCustomizer(Set<? extends Definition> definitions) {
		this.definitions = new LinkedHashSet<>(definitions);
	}

	/**
	 * Customize the application context by registering Mockito post processors.
	 * @param context the configurable application context
	 * @param mergedContextConfiguration the merged context configuration
	 */
	@Override
	public void customizeContext(ConfigurableApplicationContext context,
			MergedContextConfiguration mergedContextConfiguration) {
		if (context instanceof BeanDefinitionRegistry registry) {
			MockitoPostProcessor.register(registry, this.definitions);
		}
	}

	/**
	 * Compares this MockitoContextCustomizer object to the specified object.
	 * @param obj the object to compare to
	 * @return true if the objects are equal, false otherwise
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null || obj.getClass() != getClass()) {
			return false;
		}
		MockitoContextCustomizer other = (MockitoContextCustomizer) obj;
		return this.definitions.equals(other.definitions);
	}

	/**
	 * Returns the hash code value for this MockitoContextCustomizer object. The hash code
	 * is generated based on the hash code of the definitions field.
	 * @return the hash code value for this object
	 */
	@Override
	public int hashCode() {
		return this.definitions.hashCode();
	}

}
