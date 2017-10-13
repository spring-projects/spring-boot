/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.reflect;

/**
 * A {@code ParameterMappingException} is thrown when a failure occurs during
 * {@link ParameterMapper#mapParameter(Object, Class) operation parameter mapping}.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class ParameterMappingException extends RuntimeException {

	private final Object input;

	private final Class<?> type;

	/**
	 * Creates a new {@code ParameterMappingException} for a failure that occurred when
	 * trying to map the given {@code input} to the given {@code type}.
	 *
	 * @param input the input that was being mapped
	 * @param type the type that was being mapped to
	 * @param cause the cause of the mapping failure
	 */
	public ParameterMappingException(Object input, Class<?> type, Throwable cause) {
		super("Failed to map " + input + " of type " + input.getClass() + " to type "
				+ type, cause);
		this.input = input;
		this.type = type;
	}

	/**
	 * Returns the input that was to be mapped.
	 * @return the input
	 */
	public Object getInput() {
		return this.input;
	}

	/**
	 * Returns the type to be mapped to.
	 * @return the type
	 */
	public Class<?> getType() {
		return this.type;
	}

}
