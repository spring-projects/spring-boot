/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.cloudfoundry;

import java.util.Arrays;
import java.util.List;

/**
 * The specific access level granted to the cloud foundry user that's calling the
 * endpoints.
 *
 * @author Madhura Bhave
 * @since 2.0.0
 */
public enum AccessLevel {

	/**
	 * Restricted access to a limited set of endpoints.
	 */
	RESTRICTED("", "health", "info"),

	/**
	 * Full access to all endpoints.
	 */
	FULL;

	public static final String REQUEST_ATTRIBUTE = "cloudFoundryAccessLevel";

	private final List<String> ids;

	AccessLevel(String... ids) {
		this.ids = Arrays.asList(ids);
	}

	/**
	 * Returns if the access level should allow access to the specified ID.
	 * @param id the ID to check
	 * @return {@code true} if access is allowed
	 */
	public boolean isAccessAllowed(String id) {
		return this.ids.isEmpty() || this.ids.contains(id);
	}

}
