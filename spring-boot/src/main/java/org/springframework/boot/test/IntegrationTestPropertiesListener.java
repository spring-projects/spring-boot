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

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Manipulate the TestContext to merge properties from <code>@IntegrationTest</code> value
 * and properties attributes.
 * 
 * @author Dave Syer
 *
 */
public class IntegrationTestPropertiesListener extends AbstractTestExecutionListener {

	private String[] defaultValues = (String[]) AnnotationUtils.getDefaultValue(
			IntegrationTest.class, "properties");

	@Override
	public void prepareTestInstance(TestContext testContext) throws Exception {
		MergedContextConfiguration config = null;
		try {
			// Here be hacks...
			config = (MergedContextConfiguration) ReflectionTestUtils.getField(
					testContext, "mergedContextConfiguration");
			ReflectionTestUtils.setField(config, "propertySourceProperties",
					getEnvironmentProperties(config));
		}
		catch (IllegalStateException e) {
			throw e;
		}
		catch (Exception e) {
		}
	}

	protected String[] getEnvironmentProperties(MergedContextConfiguration config) {
		IntegrationTest annotation = AnnotationUtils.findAnnotation(
				config.getTestClass(), IntegrationTest.class);
		return mergeProperties(
				getDefaultEnvironmentProperties(config.getPropertySourceProperties(),
						annotation), getEnvironmentProperties(annotation));
	}

	private String[] getDefaultEnvironmentProperties(String[] original,
			IntegrationTest annotation) {
		String[] defaults = mergeProperties(original, defaultValues);
		if (annotation == null || defaults.length == 0) {
			// Without an @IntegrationTest we can assume the defaults are fine
			return defaults;
		}
		// If @IntegrationTest is present we don't provide a default for the server.port
		return filterPorts((String[]) AnnotationUtils.getDefaultValue(annotation,
				"properties"));
	}

	private String[] filterPorts(String[] values) {

		Set<String> result = new LinkedHashSet<String>();
		for (String value : values) {
			if (!value.contains(".port")) {
				result.add(value);
			}
		}
		return result.toArray(new String[0]);

	}

	private String[] getEnvironmentProperties(IntegrationTest annotation) {
		if (annotation == null) {
			return new String[0];
		}
		if (Arrays.asList(annotation.properties()).equals(Arrays.asList(defaultValues))) {
			return annotation.value();
		}
		if (annotation.value().length == 0) {
			return annotation.properties();
		}
		throw new IllegalStateException(
				"Either properties or value can be provided but not both");
	}

	private String[] mergeProperties(String[] original, String[] extra) {
		Set<String> result = new LinkedHashSet<String>();
		for (String value : original) {
			result.add(value);
		}
		for (String value : extra) {
			result.add(value);
		}
		return result.toArray(new String[0]);
	}

}
