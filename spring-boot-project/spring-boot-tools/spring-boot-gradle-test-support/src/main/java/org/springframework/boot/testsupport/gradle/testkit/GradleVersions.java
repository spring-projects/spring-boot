/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.testsupport.gradle.testkit;

import java.util.Arrays;
import java.util.List;

import org.gradle.api.JavaVersion;
import org.gradle.util.GradleVersion;

/**
 * Versions of Gradle used for testing.
 *
 * @author Scott Frederick
 */
public final class GradleVersions {

	private GradleVersions() {
	}

	public static List<String> allCompatible() {
		if (isJava17()) {
			return Arrays.asList("7.2", "7.3.2");
		}
		if (isJava16()) {
			return Arrays.asList("7.0.2", "7.1", "7.2", "7.3.2");
		}
		return Arrays.asList("6.8.3", GradleVersion.current().getVersion(), "7.0.2", "7.1.1", "7.2", "7.3.2");
	}

	public static String currentOrMinimumCompatible() {
		if (isJava17()) {
			return "7.3.2";
		}
		if (isJava16()) {
			return "7.0.2";
		}
		return GradleVersion.current().getVersion();
	}

	private static boolean isJava17() {
		return JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_17);
	}

	private static boolean isJava16() {
		return JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_16);
	}

}
