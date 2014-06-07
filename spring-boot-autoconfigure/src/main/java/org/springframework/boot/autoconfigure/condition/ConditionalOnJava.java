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
import org.springframework.core.JdkVersion;
import org.springframework.util.Assert;

/**
 * {@link Conditional} that matches based on the JVM version the application is running
 * on.
 * 
 * @author Oliver Gierke
 * @author Phillip Webb
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
	 */
	Range range() default Range.EQUAL_OR_NEWER;

	/**
	 * The {@link JavaVersion} to check for. Use {@link #range()} to specify whether the
	 * configured value is an upper-exclusive or lower-inclusive boundary.
	 */
	JavaVersion value();

	/**
	 * Range options.
	 */
	public enum Range {

		/**
		 * Equal to, or newer than the specified {@link JavaVersion}.
		 */
		EQUAL_OR_NEWER,

		/**
		 * Older than the specified {@link JavaVersion}.
		 */
		OLDER_THAN;

	}

	/**
	 * Java versions.
	 */
	public enum JavaVersion {

		/**
		 * Java 1.6.
		 */
		SIX(JdkVersion.JAVA_16, "1.6"),

		/**
		 * Java 1.7.
		 */
		SEVEN(JdkVersion.JAVA_17, "1.7"),

		/**
		 * Java 1.8.
		 */
		EIGHT(JdkVersion.JAVA_18, "1.8"),

		/**
		 * Java 1.9.
		 */
		NINE(JdkVersion.JAVA_19, "1.9");

		private final int value;

		private final String name;

		private JavaVersion(int value, String name) {
			this.value = value;
			this.name = name;
		}

		/**
		 * Determines if this version is within the specified range of versions.
		 * @param range the range
		 * @param version the bounds of the range
		 * @return if this version is within the specified range
		 */
		public boolean isWithin(Range range, JavaVersion version) {
			Assert.notNull(range, "Range must not be null");
			Assert.notNull(version, "Version must not be null");
			switch (range) {
			case EQUAL_OR_NEWER:
				return this.value >= version.value;
			case OLDER_THAN:
				return this.value < version.value;
			}
			throw new IllegalStateException("Unknown range " + range);
		}

		@Override
		public String toString() {
			return this.name;
		}

		/**
		 * Returns the {@link JavaVersion} of the current runtime.
		 */
		public static JavaVersion getJavaVersion() {
			int version = JdkVersion.getMajorJavaVersion();
			for (JavaVersion candidate : JavaVersion.values()) {
				if (candidate.value == version) {
					return candidate;
				}
			}
			return SIX;
		}
	}
}
