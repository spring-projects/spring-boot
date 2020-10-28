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

package org.springframework.boot.test.context;

import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContextAnnotationUtils;

/**
 * {@link ContextCustomizer} to track the web environment that is used in a
 * {@link SpringBootTest}. The web environment is taken into account when evaluating a
 * {@link MergedContextConfiguration} to determine if a context can be shared between
 * tests.
 *
 * @author Andy Wilkinson
 */
class SpringBootTestWebEnvironment implements ContextCustomizer {

	private final WebEnvironment webEnvironment;

	SpringBootTestWebEnvironment(Class<?> testClass) {
		SpringBootTest sprintBootTest = TestContextAnnotationUtils.findMergedAnnotation(testClass,
				SpringBootTest.class);
		this.webEnvironment = (sprintBootTest != null) ? sprintBootTest.webEnvironment() : null;
	}

	@Override
	public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
	}

	@Override
	public boolean equals(Object obj) {
		return (obj != null) && (getClass() == obj.getClass())
				&& this.webEnvironment == ((SpringBootTestWebEnvironment) obj).webEnvironment;
	}

	@Override
	public int hashCode() {
		return (this.webEnvironment != null) ? this.webEnvironment.hashCode() : 0;
	}

}
