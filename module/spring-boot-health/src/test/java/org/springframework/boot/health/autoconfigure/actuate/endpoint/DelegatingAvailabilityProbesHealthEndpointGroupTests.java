/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.autoconfigure.actuate.endpoint;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.health.actuate.endpoint.AdditionalHealthEndpointPath;
import org.springframework.boot.health.actuate.endpoint.HealthEndpointGroup;
import org.springframework.boot.health.actuate.endpoint.HttpCodeStatusMapper;
import org.springframework.boot.health.actuate.endpoint.StatusAggregator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DelegatingAvailabilityProbesHealthEndpointGroup}.
 *
 * @author Madhura Bhave
 * @author Taejin Han
 */
class DelegatingAvailabilityProbesHealthEndpointGroupTests {

	private HealthEndpointGroup delegate;

	private DelegatingAvailabilityProbesHealthEndpointGroup group;

	private HttpCodeStatusMapper mapper;

	private StatusAggregator aggregator;

	@BeforeEach
	void setup() {
		this.delegate = mock(HealthEndpointGroup.class);
		this.mapper = mock(HttpCodeStatusMapper.class);
		this.aggregator = mock(StatusAggregator.class);
		given(this.delegate.getHttpCodeStatusMapper()).willReturn(this.mapper);
		given(this.delegate.getStatusAggregator()).willReturn(this.aggregator);
		given(this.delegate.showComponents(any())).willReturn(true);
		given(this.delegate.showDetails(any())).willReturn(false);
		given(this.delegate.isMember("test")).willReturn(true);
		this.group = new DelegatingAvailabilityProbesHealthEndpointGroup(this.delegate,
				AdditionalHealthEndpointPath.from("server:test"), null);
	}

	@Test
	void groupDelegatesToDelegate() {
		assertThat(this.group.getHttpCodeStatusMapper()).isEqualTo(this.mapper);
		assertThat(this.group.getStatusAggregator()).isEqualTo(this.aggregator);
		assertThat(this.group.isMember("test")).isTrue();
		assertThat(this.group.showDetails(SecurityContext.NONE)).isFalse();
		assertThat(this.group.showComponents(SecurityContext.NONE)).isTrue();
		assertThat(this.group.getAdditionalPath()).isNotNull();
		assertThat(this.group.getAdditionalPath().getValue()).isEqualTo("test");
	}

	@Test
	void groupWithMembersOverrideDelegateMembership() {
		DelegatingAvailabilityProbesHealthEndpointGroup group = new DelegatingAvailabilityProbesHealthEndpointGroup(
				this.delegate, AdditionalHealthEndpointPath.from("server:test"), Set.of("livenessState"));
		assertThat(group.isMember("livenessState")).isTrue();
		assertThat(group.isMember("test")).isFalse();
	}

	@Test
	void groupWithNullAdditionalPathDelegatesToDelegateAdditionalPath() {
		given(this.delegate.getAdditionalPath()).willReturn(AdditionalHealthEndpointPath.from("server:delegated"));
		DelegatingAvailabilityProbesHealthEndpointGroup group = new DelegatingAvailabilityProbesHealthEndpointGroup(
				this.delegate, null, null);
		assertThat(group.getAdditionalPath()).isNotNull();
		assertThat(group.getAdditionalPath().getValue()).isEqualTo("delegated");
	}

}
