/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cloudfoundry.autoconfigure.actuate.endpoint;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.endpoint.annotation.DiscoveredEndpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link CloudFoundryEndpointFilter}.
 *
 * @author Madhura Bhave
 */
class CloudFoundryEndpointFilterTests {

	private final CloudFoundryEndpointFilter filter = new CloudFoundryEndpointFilter();

	@Test
	void matchIfDiscovererCloudFoundryShouldReturnFalse() {
		DiscoveredEndpoint<?> endpoint = mock(DiscoveredEndpoint.class);
		given(endpoint.wasDiscoveredBy(CloudFoundryWebEndpointDiscoverer.class)).willReturn(true);
		assertThat(this.filter.match(endpoint)).isTrue();
	}

	@Test
	void matchIfDiscovererNotCloudFoundryShouldReturnFalse() {
		DiscoveredEndpoint<?> endpoint = mock(DiscoveredEndpoint.class);
		given(endpoint.wasDiscoveredBy(CloudFoundryWebEndpointDiscoverer.class)).willReturn(false);
		assertThat(this.filter.match(endpoint)).isFalse();
	}

}
