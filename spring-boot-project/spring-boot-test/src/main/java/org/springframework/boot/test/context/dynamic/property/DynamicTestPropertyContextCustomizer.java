/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.test.context.dynamic.property;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * {@link ContextCustomizer} to allow using the {@link DynamicTestProperty} in tests.
 *
 * @author Anatoliy Korovin
 */
public class DynamicTestPropertyContextCustomizer implements ContextCustomizer {

	private Set<TestPropertyValues> properties;

	public DynamicTestPropertyContextCustomizer(List<TestPropertyValues> properties) {
		this.properties = new HashSet<>(properties);
	}

	@Override
	public void customizeContext(
			ConfigurableApplicationContext configurableApplicationContext,
			MergedContextConfiguration mergedContextConfiguration) {
		for (TestPropertyValues property : this.properties) {
			property.applyTo(configurableApplicationContext);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		DynamicTestPropertyContextCustomizer that = (DynamicTestPropertyContextCustomizer) o;
		return Objects.equals(this.properties, that.properties);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.properties);
	}

}
