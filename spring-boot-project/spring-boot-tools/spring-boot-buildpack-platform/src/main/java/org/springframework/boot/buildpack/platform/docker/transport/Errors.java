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

package org.springframework.boot.buildpack.platform.docker.transport;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Errors returned from the Docker API.
 *
 * @author Phillip Webb
 * @since 2.3.0
 */
public class Errors implements Iterable<Errors.Error> {

	private final List<Error> errors;

	@JsonCreator
	Errors(@JsonProperty("errors") List<Error> errors) {
		this.errors = (errors != null) ? errors : Collections.emptyList();
	}

	@Override
	public Iterator<Errors.Error> iterator() {
		return this.errors.iterator();
	}

	/**
	 * Returns a sequential {@code Stream} of the errors.
	 * @return a stream of the errors
	 */
	public Stream<Error> stream() {
		return this.errors.stream();
	}

	/**
	 * Return if there are any contained errors.
	 * @return if the errors are empty
	 */
	public boolean isEmpty() {
		return this.errors.isEmpty();
	}

	@Override
	public String toString() {
		return this.errors.toString();
	}

	/**
	 * An individual Docker error.
	 */
	public static class Error {

		private final String code;

		private final String message;

		@JsonCreator
		Error(String code, String message) {
			this.code = code;
			this.message = message;
		}

		/**
		 * Return the error code.
		 * @return the error code
		 */
		public String getCode() {
			return this.code;
		}

		/**
		 * Return the error message.
		 * @return the error message
		 */
		public String getMessage() {
			return this.message;
		}

		@Override
		public String toString() {
			return this.code + ": " + this.message;
		}

	}

}
