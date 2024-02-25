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

package org.springframework.boot.loader.net.protocol.jar;

/**
 * {@link ThreadLocal} state for {@link Handler} optimizations.
 *
 * @author Phillip Webb
 */
final class Optimizations {

	private static final ThreadLocal<Boolean> status = new ThreadLocal<>();

	/**
	 * This is a private constructor for the Optimizations class. It is used to prevent
	 * the instantiation of the class.
	 */
	private Optimizations() {
	}

	/**
	 * Enables the specified optimization.
	 * @param readContents true to enable the optimization, false otherwise
	 */
	static void enable(boolean readContents) {
		status.set(readContents);
	}

	/**
	 * Disables the optimization. This method removes the status.
	 */
	static void disable() {
		status.remove();
	}

	/**
	 * Returns a boolean value indicating whether the optimization is enabled or not.
	 * @return {@code true} if the optimization is enabled, {@code false} otherwise.
	 */
	static boolean isEnabled() {
		return status.get() != null;
	}

	/**
	 * Checks if the specified readContents value is equal to the current status value.
	 * @param readContents the readContents value to compare with the current status value
	 * @return true if the readContents value is equal to the current status value, false
	 * otherwise
	 */
	static boolean isEnabled(boolean readContents) {
		return Boolean.valueOf(readContents).equals(status.get());
	}

}
