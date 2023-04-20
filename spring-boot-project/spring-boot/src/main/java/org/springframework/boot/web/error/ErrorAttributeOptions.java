/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.web.error;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Options controlling the contents of {@code ErrorAttributes}.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @since 2.3.0
 */
public final class ErrorAttributeOptions {

	private final Set<Include> includes;

	private ErrorAttributeOptions(Set<Include> includes) {
		this.includes = includes;
	}

	/**
	 * Get the option for including the specified attribute in the error response.
	 * @param include error attribute to get
	 * @return {@code true} if the {@code Include} attribute is included in the error
	 * response, {@code false} otherwise
	 */
	public boolean isIncluded(Include include) {
		return this.includes.contains(include);
	}

	/**
	 * Get all options for including attributes in the error response.
	 * @return the options
	 */
	public Set<Include> getIncludes() {
		return this.includes;
	}

	/**
	 * Return an {@code ErrorAttributeOptions} that includes the specified attribute
	 * {@link Include} options.
	 * @param includes error attributes to include
	 * @return an {@code ErrorAttributeOptions}
	 */
	public ErrorAttributeOptions including(Include... includes) {
		EnumSet<Include> updated = copyIncludes();
		updated.addAll(Arrays.asList(includes));
		return new ErrorAttributeOptions(Collections.unmodifiableSet(updated));
	}

	/**
	 * Return an {@code ErrorAttributeOptions} that excludes the specified attribute
	 * {@link Include} options.
	 * @param excludes error attributes to exclude
	 * @return an {@code ErrorAttributeOptions}
	 */
	public ErrorAttributeOptions excluding(Include... excludes) {
		EnumSet<Include> updated = copyIncludes();
		updated.removeAll(Arrays.asList(excludes));
		return new ErrorAttributeOptions(Collections.unmodifiableSet(updated));
	}

	private EnumSet<Include> copyIncludes() {
		return (this.includes.isEmpty()) ? EnumSet.noneOf(Include.class) : EnumSet.copyOf(this.includes);
	}

	/**
	 * Create an {@code ErrorAttributeOptions} with defaults.
	 * @return an {@code ErrorAttributeOptions}
	 */
	public static ErrorAttributeOptions defaults() {
		return of();
	}

	/**
	 * Create an {@code ErrorAttributeOptions} that includes the specified attribute
	 * {@link Include} options.
	 * @param includes error attributes to include
	 * @return an {@code ErrorAttributeOptions}
	 */
	public static ErrorAttributeOptions of(Include... includes) {
		return of(Arrays.asList(includes));
	}

	/**
	 * Create an {@code ErrorAttributeOptions} that includes the specified attribute
	 * {@link Include} options.
	 * @param includes error attributes to include
	 * @return an {@code ErrorAttributeOptions}
	 */
	public static ErrorAttributeOptions of(Collection<Include> includes) {
		return new ErrorAttributeOptions(
				(includes.isEmpty()) ? Collections.emptySet() : Collections.unmodifiableSet(EnumSet.copyOf(includes)));
	}

	/**
	 * Error attributes that can be included in an error response.
	 */
	public enum Include {

		/**
		 * Include the exception class name attribute.
		 */
		EXCEPTION,

		/**
		 * Include the stack trace attribute.
		 */
		STACK_TRACE,

		/**
		 * Include the message attribute.
		 */
		MESSAGE,

		/**
		 * Include the binding errors attribute.
		 */
		BINDING_ERRORS

	}

}
