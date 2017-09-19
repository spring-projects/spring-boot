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

package org.springframework.boot.actuate.trace;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Include options for tracing.
 *
 * @author Wallace Wadge
 * @since 2.0.0
 */
public enum Include {

	/**
	 * Include request headers.
	 */
	REQUEST_HEADERS,

	/**
	 * Include response headers.
	 */
	RESPONSE_HEADERS,

	/**
	 * Include "Cookie" in request and "Set-Cookie" in response headers.
	 */
	COOKIES,

	/**
	 * Include authorization header (if any).
	 */
	AUTHORIZATION_HEADER,

	/**
	 * Include errors (if any).
	 */
	ERRORS,

	/**
	 * Include path info.
	 */
	PATH_INFO,

	/**
	 * Include the translated path.
	 */
	PATH_TRANSLATED,

	/**
	 * Include the context path.
	 */
	CONTEXT_PATH,

	/**
	 * Include the user principal.
	 */
	USER_PRINCIPAL,

	/**
	 * Include the parameters.
	 */
	PARAMETERS,

	/**
	 * Include the query string.
	 */
	QUERY_STRING,

	/**
	 * Include the authentication type.
	 */
	AUTH_TYPE,

	/**
	 * Include the remote address.
	 */
	REMOTE_ADDRESS,

	/**
	 * Include the session ID.
	 */
	SESSION_ID,

	/**
	 * Include the remote user.
	 */
	REMOTE_USER,

	/**
	 * Include the time taken to service the request in milliseconds.
	 */
	TIME_TAKEN;

	private static final Set<Include> DEFAULT_INCLUDES;

	static {
		Set<Include> defaultIncludes = new LinkedHashSet<>();
		defaultIncludes.add(Include.REQUEST_HEADERS);
		defaultIncludes.add(Include.RESPONSE_HEADERS);
		defaultIncludes.add(Include.COOKIES);
		defaultIncludes.add(Include.ERRORS);
		defaultIncludes.add(Include.TIME_TAKEN);
		DEFAULT_INCLUDES = Collections.unmodifiableSet(defaultIncludes);
	}

	/**
	 * Return the default {@link Include}.
	 * @return the default include.
	 */
	public static Set<Include> defaultIncludes() {
		return DEFAULT_INCLUDES;
	}

}
