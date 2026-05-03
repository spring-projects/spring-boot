/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.logging.structured;

import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.json.JsonWriter.Members;
import org.springframework.core.env.Environment;

/**
 * Customizer that can be injected into {@link StructuredLogFormatter} implementations to
 * customize {@link JsonWriter} {@link Members}.
 * <p>
 * An implementation may be provided using the {@code logging.structured.json.customizer}
 * property. Alternatively, implementations can be registered in
 * {@code META-INF/spring.factories} under the key
 * {@code org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer}.
 * <p>
 * Implementing classes can declare the following parameter types in the constructor:
 * <ul>
 * <li>{@link Environment}</li>
 * </ul>
 * When using Logback, implementing classes can also use the following parameter types in
 * the constructor:
 * <ul>
 * <li>{@code ch.qos.logback.classic.pattern.ThrowableProxyConverter}</li>
 * </ul>
 *
 * @param <T> the type being written
 * @author Phillip Webb
 * @since 3.4.0
 * @see JsonWriterStructuredLogFormatter
 */
@FunctionalInterface
public interface StructuredLoggingJsonMembersCustomizer<T> {

	/**
	 * Customize the given {@link Members} instance.
	 * @param members the members instance to customize
	 */
	void customize(JsonWriter.Members<T> members);

	/**
	 * Builder that can be injected into a {@link StructuredLogFormatter} to build the
	 * {@link StructuredLoggingJsonMembersCustomizer} when specific settings are required.
	 *
	 * @param <T> the type being written
	 * @since 3.5.4
	 */
	interface Builder<T> {

		/**
		 * Use nested fields when adding JSON from user defined properties.
		 * @return this builder
		 */
		default Builder<T> nested() {
			return nested(true);
		}

		/**
		 * Set if nested fields should be used when adding JSON from user defined
		 * properties.
		 * @param nested if nested fields are to be used
		 * @return this builder
		 */
		Builder<T> nested(boolean nested);

		/**
		 * Build the {@link StructuredLoggingJsonMembersCustomizer}.
		 * @return the built customizer
		 */
		StructuredLoggingJsonMembersCustomizer<T> build();

	}

}
