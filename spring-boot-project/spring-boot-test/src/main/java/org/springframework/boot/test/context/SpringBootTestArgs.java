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
import java.util.Set;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * {@link ContextCustomizer} to track application arguments that are used in a
 * {@link SpringBootTest}. The application arguments are taken into account when
 * evaluating a {@link MergedContextConfiguration} to determine if a context can be shared
 * between tests.
 *
 * @author Madhura Bhave
 */
class SpringBootTestArgs implements ContextCustomizer {

	private static final String[] NO_ARGS = new String[0];

	private final String[] args;

	SpringBootTestArgs(Class<?> testClass) {
		this.args = MergedAnnotations.from(testClass, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY)
				.get(SpringBootTest.class).getValue("args", String[].class).orElse(NO_ARGS);
	}

	@Override
	public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
	}

	String[] getArgs() {
		return this.args;
	}

	@Override
	public boolean equals(Object obj) {
		return (obj != null) && (getClass() == obj.getClass())
				&& Arrays.equals(this.args, ((SpringBootTestArgs) obj).args);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(this.args);
	}

	/**
	 * Return the application arguments from the given customizers.
	 * @param customizers the customizers to check
	 * @return the application args or an empty array
	 */
	static String[] get(Set<ContextCustomizer> customizers) {
		for (ContextCustomizer customizer : customizers) {
			if (customizer instanceof SpringBootTestArgs testArgs) {
				return testArgs.args;
			}
		}
		return NO_ARGS;
	}

}
