/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.autoconfigure.info;

import org.springframework.boot.actuate.autoconfigure.OnEndpointElementCondition;

/**
 * Controls the fallback behavior when the primary property that controls whether an info
 * contributor is enabled is not set.
 *
 * @author Andy Wilkinson
 * @since 2.6.0
 * @see OnEndpointElementCondition
 */
public enum InfoContributorFallback {

	/**
	 * Fall back to the {@code management.info.defaults.enabled} property, matching if it
	 * is {@code true} or if it is not configured.
	 */
	USE_DEFAULTS_PROPERTY,

	/**
	 * Do not fall back, thereby disabling the info contributor.
	 */
	DISABLE

}
