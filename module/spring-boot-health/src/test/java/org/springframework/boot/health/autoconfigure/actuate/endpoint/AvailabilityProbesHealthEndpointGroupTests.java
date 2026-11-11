/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.autoconfigure.actuate.endpoint;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.health.actuate.endpoint.HttpCodeStatusMapper;
import org.springframework.boot.health.actuate.endpoint.StatusAggregator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AvailabilityProbesHealthEndpointGroup}.
 *
 * @author Phillip Webb
 */
class AvailabilityProbesHealthEndpointGroupTests {

	private final AvailabilityProbesHealthEndpointGroup group = new AvailabilityProbesHealthEndpointGroup(null, "a",
			"b");

	@Test
	void isMemberWhenMemberReturnsTrue() {
		assertThat(this.group.isMember("a")).isTrue();
		assertThat(this.group.isMember("b")).isTrue();
	}

	@Test
	void isMemberWhenNotMemberReturnsFalse() {
		assertThat(this.group.isMember("c")).isFalse();
	}

	@Test
	void showComponentsReturnsFalse() {
		assertThat(this.group.showComponents(mock(SecurityContext.class))).isFalse();
	}

	@Test
	void showDetailsReturnsFalse() {
		assertThat(this.group.showDetails(mock(SecurityContext.class))).isFalse();
	}

	@Test
	void getStatusAggregatorReturnsDefaultStatusAggregator() {
		assertThat(this.group.getStatusAggregator()).isEqualTo(StatusAggregator.getDefault());
	}

	@Test
	void getHttpCodeStatusMapperReturnsDefaultHttpCodeStatusMapper() {
		assertThat(this.group.getHttpCodeStatusMapper()).isEqualTo(HttpCodeStatusMapper.getDefault());
	}

}
