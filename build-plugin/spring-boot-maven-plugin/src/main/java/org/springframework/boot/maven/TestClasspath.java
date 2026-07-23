/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.maven;

import java.util.Locale;

/**
 * Use of the test classpath when running an application.
 *
 * @author Henrique (henriquejsza)
 */
enum TestClasspath {

	OFF(false, false),

	DEPENDENCIES(true, false),

	ALL(true, true);

	private final boolean useTestDependencies;

	private final boolean useTestClasses;

	TestClasspath(boolean useTestDependencies, boolean useTestClasses) {
		this.useTestDependencies = useTestDependencies;
		this.useTestClasses = useTestClasses;
	}

	boolean isUseTestDependencies() {
		return this.useTestDependencies;
	}

	boolean isUseTestClasses() {
		return this.useTestClasses;
	}

	static TestClasspath of(String value) {
		if ("true".equalsIgnoreCase(value)) {
			return DEPENDENCIES;
		}
		if ("false".equalsIgnoreCase(value)) {
			return OFF;
		}
		try {
			return valueOf(value.toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException ex) {
			String message = "Unsupported test classpath '%s'. Valid values are OFF, DEPENDENCIES, ALL, true, and false"
				.formatted(value);
			throw new IllegalArgumentException(message, ex);
		}
	}

}
