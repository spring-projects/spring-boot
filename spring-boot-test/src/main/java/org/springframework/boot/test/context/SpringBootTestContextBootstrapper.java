/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.test.context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContextBootstrapper;
import org.springframework.test.context.support.DefaultTestContextBootstrapper;
import org.springframework.util.Assert;

/**
 * {@link TestContextBootstrapper} that uses {@link SpringApplicationContextLoader} and
 * can automatically find the {@link SpringBootConfiguration @SpringBootConfiguration}.
 *
 * @author Phillip Webb
 * @since 1.4.0
 * @see TestConfiguration
 */
public class SpringBootTestContextBootstrapper extends DefaultTestContextBootstrapper {

	private static final Log logger = LogFactory
			.getLog(SpringBootTestContextBootstrapper.class);

	@Override
	protected Class<? extends ContextLoader> getDefaultContextLoaderClass(
			Class<?> testClass) {
		return SpringApplicationContextLoader.class;
	}

	@Override
	protected MergedContextConfiguration processMergedContextConfiguration(
			MergedContextConfiguration mergedConfig) {
		Class<?>[] classes = getOrFindConfigurationClasses(mergedConfig);
		List<String> propertySourceProperties = getAndProcessPropertySourceProperties(
				mergedConfig);
		return new MergedContextConfiguration(mergedConfig.getTestClass(),
				mergedConfig.getLocations(), classes,
				mergedConfig.getContextInitializerClasses(),
				mergedConfig.getActiveProfiles(),
				mergedConfig.getPropertySourceLocations(),
				propertySourceProperties
						.toArray(new String[propertySourceProperties.size()]),
				mergedConfig.getContextCustomizers(), mergedConfig.getContextLoader(),
				getCacheAwareContextLoaderDelegate(), mergedConfig.getParent());
	}

	protected Class<?>[] getOrFindConfigurationClasses(
			MergedContextConfiguration mergedConfig) {
		Class<?>[] classes = mergedConfig.getClasses();
		if (containsNonTestComponent(classes) || mergedConfig.hasLocations()
				|| !mergedConfig.getContextInitializerClasses().isEmpty()) {
			return classes;
		}
		Class<?> found = new SpringBootConfigurationFinder()
				.findFromClass(mergedConfig.getTestClass());
		Assert.state(found != null,
				"Unable to find a @SpringBootConfiguration, you need to use "
						+ "@ContextConfiguration or @SpringApplicationConfiguration "
						+ "with your test");
		logger.info("Found @SpringBootConfiguration " + found.getName() + " for test "
				+ mergedConfig.getTestClass());
		return merge(found, classes);
	}

	private boolean containsNonTestComponent(Class<?>[] classes) {
		for (Class<?> candidate : classes) {
			if (!AnnotatedElementUtils.isAnnotated(candidate, TestConfiguration.class)) {
				return true;
			}
		}
		return false;
	}

	private Class<?>[] merge(Class<?> head, Class<?>[] existing) {
		Class<?>[] result = new Class<?>[existing.length + 1];
		result[0] = head;
		System.arraycopy(existing, 0, result, 1, existing.length);
		return result;
	}

	private List<String> getAndProcessPropertySourceProperties(
			MergedContextConfiguration mergedConfig) {
		List<String> propertySourceProperties = new ArrayList<String>(
				Arrays.asList(mergedConfig.getPropertySourceProperties()));
		String differentiator = getDifferentiatorPropertySourceProperty();
		if (differentiator != null) {
			propertySourceProperties.add(differentiator);
		}
		processPropertySourceProperties(mergedConfig, propertySourceProperties);
		return propertySourceProperties;
	}

	/**
	 * Return a "differentiator" property to ensure that there is something to
	 * differentiate regular tests and bootstrapped tests. Without this property a cached
	 * context could be returned that wasn't created by this bootstrapper. By default uses
	 * the bootstrapper class as a property.
	 * @return the differentiator or {@code null}
	 */
	protected String getDifferentiatorPropertySourceProperty() {
		return getClass().getName() + "=true";
	}

	/**
	 * Post process the property source properties, adding or removing elements as
	 * required.
	 * @param mergedConfig the merged context configuration
	 * @param propertySourceProperties the property source properties to process
	 */
	protected void processPropertySourceProperties(
			MergedContextConfiguration mergedConfig,
			List<String> propertySourceProperties) {
	}

}
