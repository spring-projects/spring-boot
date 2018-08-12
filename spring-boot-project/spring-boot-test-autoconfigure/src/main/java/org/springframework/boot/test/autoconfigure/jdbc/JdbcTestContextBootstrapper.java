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

package org.springframework.boot.test.autoconfigure.jdbc;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContextBootstrapper;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@link TestContextBootstrapper} for {@link JdbcTest @JdbcTest} support.
 *
 * @author Artsiom Yudovin
 */
public class JdbcTestContextBootstrapper extends SpringBootTestContextBootstrapper {

	@Override
	protected MergedContextConfiguration processMergedContextConfiguration(
			MergedContextConfiguration mergedConfig) {
		Class<?>[] classes = getOrFindConfigurationClasses(mergedConfig);
		List<String> propertySourceProperties = getAndProcessPropertySourceProperties(
				mergedConfig);
		mergedConfig = createModifiedConfig(mergedConfig, classes,
				StringUtils.toStringArray(propertySourceProperties));

		return mergedConfig;
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
		Class<?> testClass = mergedConfig.getTestClass();
		String[] properties = getJdbcTestProperties(testClass);
		if (!ObjectUtils.isEmpty(properties)) {
			// Added first so that inlined properties from @TestPropertySource take
			// precedence
			propertySourceProperties.addAll(0, Arrays.asList(properties));
		}
	}

	protected String[] getJdbcTestProperties(Class<?> testClass) {
		JdbcTest annotation = getJdbcAnnotation(testClass);
		return (annotation != null) ? annotation.properties() : null;
	}

	protected JdbcTest getJdbcAnnotation(Class<?> testClass) {
		return AnnotatedElementUtils.getMergedAnnotation(testClass, JdbcTest.class);
	}

}
