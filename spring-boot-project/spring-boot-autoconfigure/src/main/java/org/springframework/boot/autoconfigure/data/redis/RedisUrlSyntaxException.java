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

package org.springframework.boot.autoconfigure.data.redis;

/**
 * Exception thrown when a Redis URL is malformed or invalid.
 *
 * @author Scott Frederick
 */
class RedisUrlSyntaxException extends RuntimeException {

	private final String url;

	/**
	 * Constructs a new RedisUrlSyntaxException with the specified URL and cause.
	 * @param url the URL that caused the exception
	 * @param cause the exception that caused this exception to be thrown
	 */
	RedisUrlSyntaxException(String url, Exception cause) {
		super(buildMessage(url), cause);
		this.url = url;
	}

	/**
	 * Constructs a new RedisUrlSyntaxException with the specified URL.
	 * @param url the URL that caused the exception
	 */
	RedisUrlSyntaxException(String url) {
		super(buildMessage(url));
		this.url = url;
	}

	/**
	 * Returns the URL associated with this RedisUrlSyntaxException.
	 * @return the URL associated with this RedisUrlSyntaxException
	 */
	String getUrl() {
		return this.url;
	}

	/**
	 * Builds an error message for an invalid Redis URL.
	 * @param url the invalid Redis URL
	 * @return the error message
	 */
	private static String buildMessage(String url) {
		return "Invalid Redis URL '" + url + "'";
	}

}
