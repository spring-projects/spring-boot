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
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.Future;

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
	 * Java 17.
	 */
	SEVENTEEN("17", Console.class, "charset"),

	/**
	 * Java 18.
	 */
	EIGHTEEN("18", Duration.class, "isPositive"),

	/**
	 * Java 19.
	 */
	NINETEEN("19", Future.class, "state"),

	/**
	 * Java 20.
	 */
	TWENTY("20", Class.class, "accessFlags"),

	/**
	 * Java 21.
	 */
	TWENTY_ONE("21", SortedSet.class, "getFirst");

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
		return Arrays.stream(JavaVersion.values())
				.sorted(Collections.reverseOrder())
				.filter((javaVersion) -> javaVersion.available)
				.findFirst()
				.orElse(SEVENTEEN);
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
