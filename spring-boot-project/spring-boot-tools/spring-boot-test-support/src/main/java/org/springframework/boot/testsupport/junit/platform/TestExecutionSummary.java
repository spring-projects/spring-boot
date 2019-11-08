/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.testsupport.junit.platform;

import java.util.List;

import org.springframework.util.CollectionUtils;

/**
 * Reflective mirror of JUnit 5's {@code TestExecutionSummary}.
 *
 * @author Phillip Webb
 * @since 2.2.0
 */
public class TestExecutionSummary extends ReflectiveWrapper {

	private final Class<?> failureType;

	private final Object instance;

	TestExecutionSummary(ClassLoader classLoader, Object instance) throws Throwable {
		super(classLoader, "org.junit.platform.launcher.listeners.TestExecutionSummary");
		this.failureType = loadClass("org.junit.platform.launcher.listeners.TestExecutionSummary$Failure");
		this.instance = instance;
	}

	public Throwable getFailure() throws Throwable {
		List<?> failures = (List<?>) this.type.getMethod("getFailures").invoke(this.instance);
		if (!CollectionUtils.isEmpty(failures)) {
			Object failure = failures.get(0);
			return (Throwable) this.failureType.getMethod("getException").invoke(failure);
		}
		return null;
	}

}
