/*
 * Copyright 2012-present the original author or authors.
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
