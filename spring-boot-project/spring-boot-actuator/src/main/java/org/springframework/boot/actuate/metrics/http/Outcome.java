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

package org.springframework.boot.actuate.metrics.http;

import io.micrometer.core.instrument.Tag;

/**
 * The outcome of an HTTP request.
 *
 * @author Andy Wilkinson
 * @since 2.2.0
 */
public enum Outcome {

	/**
	 * Outcome of the request was informational.
	 */
	INFORMATIONAL,

	/**
	 * Outcome of the request was success.
	 */
	SUCCESS,

	/**
	 * Outcome of the request was redirection.
	 */
	REDIRECTION,

	/**
	 * Outcome of the request was client error.
	 */
	CLIENT_ERROR,

	/**
	 * Outcome of the request was server error.
	 */
	SERVER_ERROR,

	/**
	 * Outcome of the request was unknown.
	 */
	UNKNOWN;

	private final Tag tag;

	Outcome() {
		this.tag = Tag.of("outcome", name());
	}

	/**
	 * Returns the {@code Outcome} as a {@link Tag} named {@code outcome}.
	 * @return the {@code outcome} {@code Tag}
	 */
	public Tag asTag() {
		return this.tag;
	}

	/**
	 * Return the {@code Outcome} for the given HTTP {@code status} code.
	 * @param status the HTTP status code
	 * @return the matching Outcome
	 */
	public static Outcome forStatus(int status) {
		if (status >= 100 && status < 200) {
			return INFORMATIONAL;
		}
		else if (status >= 200 && status < 300) {
			return SUCCESS;
		}
		else if (status >= 300 && status < 400) {
			return REDIRECTION;
		}
		else if (status >= 400 && status < 500) {
			return CLIENT_ERROR;
		}
		else if (status >= 500 && status < 600) {
			return SERVER_ERROR;
		}
		return UNKNOWN;
	}

}
