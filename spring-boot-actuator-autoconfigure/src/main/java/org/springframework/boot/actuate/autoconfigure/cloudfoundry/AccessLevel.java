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

package org.springframework.boot.actuate.autoconfigure.cloudfoundry;

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

/**
 * The specific access level granted to the cloud foundry user that's calling the
 * endpoints.
 *
 * @author Madhura Bhave
 */
enum AccessLevel {

	/**
	 * Restricted access to a limited set of endpoints.
	 */
	RESTRICTED("", "health", "info"),

	/**
	 * Full access to all endpoints.
	 */
	FULL;

	private static final String REQUEST_ATTRIBUTE = "cloudFoundryAccessLevel";

	private final List<String> endpointPaths;

	AccessLevel(String... endpointPaths) {
		this.endpointPaths = Arrays.asList(endpointPaths);
	}

	/**
	 * Returns if the access level should allow access to the specified endpoint path.
	 * @param endpointPath the endpoint path
	 * @return {@code true} if access is allowed
	 */
	public boolean isAccessAllowed(String endpointPath) {
		return this.endpointPaths.isEmpty() || this.endpointPaths.contains(endpointPath);
	}

	public void put(HttpServletRequest request) {
		request.setAttribute(REQUEST_ATTRIBUTE, this);
	}

	public static AccessLevel get(HttpServletRequest request) {
		return (AccessLevel) request.getAttribute(REQUEST_ATTRIBUTE);
	}

}
