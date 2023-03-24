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

package org.springframework.boot.test.context;

import java.util.Arrays;
import java.util.Objects;

import org.springframework.boot.test.context.SpringBootTest.UseMainMethod;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContextAnnotationUtils;

/**
 * {@link ContextCustomizer} to track attributes of
 * {@link SpringBootTest @SptringBootTest} that are taken into account when evaluating a
 * {@link MergedContextConfiguration} to determine if a context can be shared between
 * tests.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author Andy Wilkinson
 */
class SpringBootTestAnnotation implements ContextCustomizer {

	private static final String[] NO_ARGS = new String[0];

	private static final SpringBootTestAnnotation DEFAULT = new SpringBootTestAnnotation((SpringBootTest) null);

	private final String[] args;

	private final WebEnvironment webEnvironment;

	private final UseMainMethod useMainMethod;

	SpringBootTestAnnotation(Class<?> testClass) {
		this(TestContextAnnotationUtils.findMergedAnnotation(testClass, SpringBootTest.class));
	}

	private SpringBootTestAnnotation(SpringBootTest annotation) {
		this.args = (annotation != null) ? annotation.args() : NO_ARGS;
		this.webEnvironment = (annotation != null) ? annotation.webEnvironment() : WebEnvironment.NONE;
		this.useMainMethod = (annotation != null) ? annotation.useMainMethod() : UseMainMethod.NEVER;
	}

	@Override
	public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		SpringBootTestAnnotation other = (SpringBootTestAnnotation) obj;
		boolean result = Arrays.equals(this.args, other.args);
		result = result && this.useMainMethod == other.useMainMethod;
		result = result && this.webEnvironment == other.webEnvironment;
		return result;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(this.args);
		result = prime * result + Objects.hash(this.useMainMethod, this.webEnvironment);
		return result;
	}

	String[] getArgs() {
		return this.args;
	}

	WebEnvironment getWebEnvironment() {
		return this.webEnvironment;
	}

	UseMainMethod getUseMainMethod() {
		return this.useMainMethod;
	}

	/**
	 * Return the application arguments from the given {@link MergedContextConfiguration}.
	 * @param mergedConfig the merged config to check
	 * @return a {@link SpringBootTestAnnotation} instance
	 */
	static SpringBootTestAnnotation get(MergedContextConfiguration mergedConfig) {
		for (ContextCustomizer customizer : mergedConfig.getContextCustomizers()) {
			if (customizer instanceof SpringBootTestAnnotation annotation) {
				return annotation;
			}
		}
		return DEFAULT;
	}

}
