/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.actuate.endpoint;

/**
 * Hook that allows for custom modification of {@link HealthEndpointGroups} &mdash; for
 * example, automatically adding additional auto-configured groups.
 *
 * @author Phillip Webb
 * @author Brian Clozel
 * @since 4.0.0
 */
@FunctionalInterface
public interface HealthEndpointGroupsPostProcessor {

	/**
	 * Post-process the given {@link HealthEndpointGroups} instance.
	 * @param groups the existing groups instance
	 * @return a post-processed groups instance, or the original instance if not
	 * post-processing was required
	 */
	HealthEndpointGroups postProcessHealthEndpointGroups(HealthEndpointGroups groups);

}
