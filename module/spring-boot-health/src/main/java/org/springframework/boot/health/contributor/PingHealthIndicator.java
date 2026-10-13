/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.contributor;

/**
 * Auto-configured {@link HealthIndicator} that always returns {@link Status#UP}.
 *
 * @author Dave Syer
 * @author Christian Dupuis
 * @since 4.0.0
 * @see Status#UP
 */
public class PingHealthIndicator extends AbstractHealthIndicator {

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		builder.up();
	}

}
