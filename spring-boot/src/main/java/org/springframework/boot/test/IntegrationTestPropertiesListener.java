/*
 * Copyright 2013-2104 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.boot.test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Manipulate the TestContext to merge properties from {@code @IntegrationTest}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @since 1.2.0
 */
class IntegrationTestPropertiesListener extends AbstractTestExecutionListener {

	private static final String ANNOTATION_TYPE = IntegrationTest.class.getName();

	@Override
	public void prepareTestInstance(TestContext testContext) throws Exception {
		Class<?> testClass = testContext.getTestClass();
		if (AnnotatedElementUtils.isAnnotated(testClass, ANNOTATION_TYPE)) {
			AnnotationAttributes annotationAttributes = AnnotatedElementUtils
					.getAnnotationAttributes(testClass, ANNOTATION_TYPE);
			addPropertySourceProperties(testContext,
					annotationAttributes.getStringArray("value"));
		}
	}

	private void addPropertySourceProperties(TestContext testContext, String[] properties) {
		try {
			addPropertySourcePropertiesUsingReflection(testContext, properties);
		}
		catch (RuntimeException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private void addPropertySourcePropertiesUsingReflection(TestContext testContext,
			String[] properties) throws Exception {
		MergedContextConfiguration configuration = (MergedContextConfiguration) ReflectionTestUtils
				.getField(testContext, "mergedContextConfiguration");
		Set<String> merged = new LinkedHashSet<String>((Arrays.asList(configuration
				.getPropertySourceProperties())));
		merged.addAll(Arrays.asList(properties));
		addIntegrationTestProperty(merged);
		ReflectionTestUtils.setField(configuration, "propertySourceProperties",
				merged.toArray(new String[merged.size()]));
	}

	/**
	 * Add an "IntegrationTest" property to ensure that there is something to
	 * differentiate regular tests and {@code @IntegrationTest} tests. Without this
	 * property a cached context could be returned that hadn't started the embedded
	 * servlet container.
	 * @param propertySourceProperties the property source properties
	 */
	private void addIntegrationTestProperty(Set<String> propertySourceProperties) {
		propertySourceProperties.add(IntegrationTest.class.getName() + "=true");
	}

}
