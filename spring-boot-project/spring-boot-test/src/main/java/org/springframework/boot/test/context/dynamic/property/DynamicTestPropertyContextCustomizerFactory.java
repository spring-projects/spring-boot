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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;

/**
 * {@link ContextCustomizerFactory} to allow using the {@link DynamicTestProperty} in
 * tests.
 *
 * @author Anatoliy Korovin
 */
public class DynamicTestPropertyContextCustomizerFactory
		implements ContextCustomizerFactory {

	@Override
	public ContextCustomizer createContextCustomizer(Class<?> aClass,
			List<ContextConfigurationAttributes> list) {

		List<TestPropertyValues> properties = new ArrayList<>();

		for (Method method : aClass.getDeclaredMethods()) {

			if (!method.isAnnotationPresent(DynamicTestProperty.class)) {
				continue;
			}

			if (!Modifier.isStatic(method.getModifiers())) {
				throw new DynamicTestPropertyException(
						"Annotation DynamicTestProperty must be used on a static method.");
			}

			if (!method.getReturnType().equals(TestPropertyValues.class)) {
				throw new DynamicTestPropertyException(
						"DynamicTestProperty method must return the instance of TestPropertyValues.");
			}

			properties.add(getDynamicPropertyValues(method));
		}

		return properties.isEmpty() ? null
				: new DynamicTestPropertyContextCustomizer(properties);
	}

	private TestPropertyValues getDynamicPropertyValues(Method method) {
		try {
			method.setAccessible(true);
			return (TestPropertyValues) method.invoke(null);
		}
		catch (Exception ex) {
			throw new DynamicTestPropertyException(
					"Error while trying to get a value of dynamic properties.", ex);
		}
	}

}
