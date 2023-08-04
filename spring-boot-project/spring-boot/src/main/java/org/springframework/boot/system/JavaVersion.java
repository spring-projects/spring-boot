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

package org.springframework.boot.system;

import java.io.Console;
import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import org.springframework.util.ClassUtils;

/**
 * Known Java versions.
 *
 * @author Oliver Gierke
 * @author Phillip Webb
 * @author Moritz Halbritter
 * @since 2.0.0
 */
public enum JavaVersion {

	/**
	 * Java 1.8.
	 * @since 2.0.0
	 */
	EIGHT("1.8", Optional.class, "empty"),

	/**
	 * Java 9.
	 * @since 2.0.0
	 */
	NINE("9", Optional.class, "stream"),

	/**
	 * Java 10.
	 * @since 2.1.7
	 */
	TEN("10", Optional.class, "orElseThrow"),

	/**
	 * Java 11.
	 * @since 2.1.7
	 */
	ELEVEN("11", String.class, "strip"),

	/**
	 * Java 12.
	 * @since 2.1.7
	 */
	TWELVE("12", String.class, "describeConstable"),

	/**
	 * Java 13.
	 * @since 2.1.7
	 */
	THIRTEEN("13", String.class, "stripIndent"),

	/**
	 * Java 14.
	 * @since 2.3.0
	 */
	FOURTEEN("14", MethodHandles.Lookup.class, "hasFullPrivilegeAccess"),

	/**
	 * Java 15.
	 * @since 2.3.0
	 */
	FIFTEEN("15", CharSequence.class, "isEmpty"),

	/**
	 * Java 16.
	 * @since 2.4.2
	 */
	SIXTEEN("16", Stream.class, "toList"),

	/**
	 * Java 17.
	 * @since 2.5.3
	 */
	SEVENTEEN("17", Console.class, "charset"),

	/**
	 * Java 18.
	 * @since 2.5.11
	 */
	EIGHTEEN("18", Duration.class, "isPositive"),

	/**
	 * Java 19.
	 * @since 2.6.12
	 */
	NINETEEN("19", Future.class, "state"),

	/**
	 * Java 20.
	 * @since 2.7.13
	 */
	TWENTY("20", Class.class, "accessFlags");

	private final String name;

	private final boolean available;

	JavaVersion(String name, Class<?> clazz, String methodName) {
		this.name = name;
		this.available = ClassUtils.hasMethod(clazz, methodName);
	}

	@Override
	public String toString() {
		return this.name;
	}

	/**
	 * Returns the {@link JavaVersion} of the current runtime.
	 * @return the {@link JavaVersion}
	 */
	public static JavaVersion getJavaVersion() {
		List<JavaVersion> candidates = Arrays.asList(JavaVersion.values());
		Collections.reverse(candidates);
		for (JavaVersion candidate : candidates) {
			if (candidate.available) {
				return candidate;
			}
		}
		return EIGHT;
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

}
