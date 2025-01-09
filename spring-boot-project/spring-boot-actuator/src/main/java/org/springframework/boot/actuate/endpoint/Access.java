/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import org.springframework.util.Assert;

/**
 * Permitted level of access to an endpoint and its operations.
 *
 * @author Andy Wilkinson
 * @since 3.4.0
 */
public enum Access {

	/**
	 * No access to the endpoint is permitted.
	 */
	NONE,

	/**
	 * Read-only access to the endpoint is permitted.
	 */
	READ_ONLY,

	/**
	 * Unrestricted access to the endpoint is permitted.
	 */
	UNRESTRICTED;

	/**
	 * Cap access to a maximum permitted.
	 * @param maxPermitted the maximum permitted access
	 * @return this access if less than the maximum or the maximum permitted
	 */
	public Access cap(Access maxPermitted) {
		Assert.notNull(maxPermitted, "'maxPermitted' must not be null");
		return (ordinal() <= maxPermitted.ordinal()) ? this : maxPermitted;
	}

}
