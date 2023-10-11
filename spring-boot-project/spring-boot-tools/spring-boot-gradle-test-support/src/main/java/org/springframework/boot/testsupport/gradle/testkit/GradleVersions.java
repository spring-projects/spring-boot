/*
 * Copyright 2012-2023 the original author or authors.
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

	@SuppressWarnings("UnstableApiUsage")
	public static List<String> allCompatible() {
		if (isJavaVersion(JavaVersion.VERSION_20)) {
			return Arrays.asList("8.1.1", "8.3", "8.4");
		}
		return Arrays.asList("7.5.1", GradleVersion.current().getVersion(), "8.0.2", "8.3", "8.4");
	}

	public static String minimumCompatible() {
		return allCompatible().get(0);
	}

	public static String maximumCompatible() {
		List<String> versions = allCompatible();
		return versions.get(versions.size() - 1);
	}

	private static boolean isJavaVersion(JavaVersion version) {
		return JavaVersion.current().isCompatibleWith(version);
	}

}
