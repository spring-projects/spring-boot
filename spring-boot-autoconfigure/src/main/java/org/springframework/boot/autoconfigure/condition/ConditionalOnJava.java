/*
 * Copyright 2012-2014 the original author or authors.
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
package org.springframework.boot.autoconfigure.condition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Conditional;
import org.springframework.util.Assert;

/**
 * {@link Conditional} that matches based on the JVM version the application is running
 * on.
 *
 * @author Oliver Gierke
 * @since 1.1.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnJavaCondition.class)
public @interface ConditionalOnJava {

	/**
	 * Configures whether the value configured in {@link #value()} shall be considered the
	 * upper exclusive or lower inclusive boundary. Defaults to
	 * {@link Range#EQUAL_OR_NEWER}.
	 * @return the range of the version
	 */
	Range range() default Range.EQUAL_OR_NEWER;

	/**
	 * The {@link JavaVersion} to check for. Use {@link #range()} to specify whether the
	 * configured value is an upper-exclusive or lower-inclusive boundary.
	 */
	JavaVersion value();

	public enum Range {

		OLDER_THAN("older than %s"), EQUAL_OR_NEWER("%s or newer");

		private final String message;

		private Range(String message) {
			this.message = message;
		}

		public String getMessage(JavaVersion version) {
			return String.format(this.message, version);
		}
	}

	/**
	 * An enum to abstract major Java versions.
	 */
	public enum JavaVersion {

		FIVE("1.5"), SIX("1.6"), SEVEN("1.7"), EIGHT("1.8"), NINE("1.9");

		private String value;

		private JavaVersion(String value) {
			this.value = value;
		}

		/**
		 * Returns the {@link JavaVersion} of the current runtime.
		 */
		public static JavaVersion fromRuntime() {

			String source = System.getProperty("java.version");

			for (JavaVersion version : JavaVersion.values()) {
				if (source.startsWith(version.value)) {
					return version;
				}
			}

			throw new IllegalArgumentException(String.format(
					"Could not detect Java version for %s.", source));
		}

		/**
		 * Returns whether the given {@link JavaVersion} is considered equal or better
		 * than the given one.
		 *
		 * @param version must not be {code null}.
		 */
		public boolean isEqualOrBetter(JavaVersion version) {

			Assert.notNull(version, "Java version must not be null!");
			return this.value.compareTo(version.value) >= 0;
		}

		@Override
		public String toString() {
			return this.value;
		}
	}
}
