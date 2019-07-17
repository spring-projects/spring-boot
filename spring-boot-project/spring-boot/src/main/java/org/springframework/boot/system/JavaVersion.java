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

package org.springframework.boot.system;

import java.lang.reflect.Method;

import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Supported Java versions.
 *
 * @author Oliver Gierke
 * @author Phillip Webb
 * @since 2.0.0
 */
public enum JavaVersion {

	/**
	 * Java 1.8.
	 */
	EIGHT("1.8"),

	/**
	 * Java 9.
	 */
	NINE("9"),

	/**
	 * Java 10.
	 * @since 2.2.0
	 */
	TEN("10"),

	/**
	 * Java 11.
	 * @since 2.2.0
	 */
	ELEVEN("11"),

	/**
	 * Java 12.
	 * @since 2.2.0
	 */
	TWELVE("12"),
	/**
	 * Java 13.
	 * @since 2.2.0
	 */
	THIRTEEN("13");

	private static final JavaVersion CURRENT = determineJavaVersion();

	private final String name;

	JavaVersion(String name) {
		this.name = name;
	}

	/**
	 * Returns the {@link JavaVersion} of the current runtime.
	 * @return the {@link JavaVersion}
	 */
	public static JavaVersion getJavaVersion() {
		JavaVersion javaVersion = CURRENT;
		if (javaVersion != null) {
			return javaVersion;
		}
		throw new IllegalStateException("Java Version is not supported");
	}

	@Override
	public String toString() {
		return this.name;
	}

	/**
	 * Return if this version is equal to or newer than a given version.
	 * @param version the version to compare
	 * @return {@code true} if this version is equal to or newer than {@code version}
	 */
	public boolean isEqualOrNewerThan(JavaVersion version) {
		return compareTo(version) >= 0;
	}

	/**
	 * Return if this version is older than a given version.
	 * @param version the version to compare
	 * @return {@code true} if this version is older than {@code version}
	 */
	public boolean isOlderThan(JavaVersion version) {
		return compareTo(version) < 0;
	}

	private static JavaVersion determineJavaVersion() {
		switch (version()) {
		case 8:
			return EIGHT;
		case 9:
			return NINE;
		case 10:
			return TEN;
		case 11:
			return ELEVEN;
		case 12:
			return TWELVE;
		case 13:
			return THIRTEEN;
		default:
			return null;
		}
	}

	private static int version() {
		Object version = getVersion();
		if (version != null) {
			return getFeature(version);
		}
		return 8;
	}

	private static Object getVersion() {
		Method method = ReflectionUtils.findMethod(Runtime.class, "version");
		if (method != null) {
			return ReflectionUtils.invokeMethod(method, null);
		}
		return null;
	}

	private static int getFeature(Object version) {
		Method method = ReflectionUtils.findMethod(version.getClass(), "feature");
		if (method != null) {
			method = ReflectionUtils.findMethod(version.getClass(), "major");
		}
		Assert.state(method != null, "java.lang.Runtime$Version instance does not have 'feature' and 'major' methods");
		return (int) ReflectionUtils.invokeMethod(method, version);
	}

}
