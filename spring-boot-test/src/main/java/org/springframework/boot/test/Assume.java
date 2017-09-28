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

package org.springframework.boot.test;

import org.junit.AssumptionViolatedException;

import org.springframework.boot.system.JavaVersion;

/**
 * Provides utility methods that allow JUnit tests to {@link org.junit.Assume} certain
 * conditions hold {@code true}. If the assumption fails, it means the test should be
 * skipped.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public abstract class Assume {

	/**
	 * Assume that the specified {@link JavaVersion} is the one currently available.
	 * @param version the expected Java version
	 * @throws AssumptionViolatedException if the assumption fails
	 */
	public static void javaVersion(JavaVersion version) {
		JavaVersion currentVersion = JavaVersion.getJavaVersion();
		boolean outcome = currentVersion.compareTo(JavaVersion.NINE) < 0;
		org.junit.Assume.assumeTrue(String.format(
				"This test should run on %s (got %s)", version, currentVersion), outcome);
	}

}
