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

package org.springframework.boot.testsupport;

import org.junit.AssumptionViolatedException;

import org.springframework.util.ClassUtils;

/**
 * Provides utility methods that allow JUnit tests to {@link org.junit.Assume} certain
 * conditions hold {@code true}. If the assumption fails, it means the test should be
 * skipped.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public abstract class Assume {

	public static void javaEight() {
		if (ClassUtils.isPresent("java.security.cert.URICertStoreParameters", null)) {
			throw new AssumptionViolatedException("Assumed Java 8 but got Java 9");
		}
	}

}
