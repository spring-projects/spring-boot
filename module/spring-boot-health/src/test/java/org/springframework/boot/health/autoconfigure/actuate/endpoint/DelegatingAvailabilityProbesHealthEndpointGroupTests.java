/*
 * Copyright 2012-present the original author or authors.
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
