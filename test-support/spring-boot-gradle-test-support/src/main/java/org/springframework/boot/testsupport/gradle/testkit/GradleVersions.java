/*
 * Copyright 2012-present the original author or authors.
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
		if (isJavaVersion(JavaVersion.VERSION_27)) {
			return Arrays.asList(GradleVersion.current().getVersion());
		}
		if (isJavaVersion(JavaVersion.VERSION_26)) {
			return Arrays.asList("9.4.0", GradleVersion.current().getVersion());
		}
		if (isJavaVersion(JavaVersion.VERSION_25)) {
			return Arrays.asList("9.0.0", GradleVersion.current().getVersion());
		}
		return Arrays.asList("8.14.5", "9.0.0", GradleVersion.current().getVersion());
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
