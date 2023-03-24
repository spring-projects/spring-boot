/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.availability;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.health.AdditionalHealthEndpointPath;
import org.springframework.boot.actuate.health.HealthEndpointGroup;
import org.springframework.boot.actuate.health.HttpCodeStatusMapper;
import org.springframework.boot.actuate.health.StatusAggregator;

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

	private DelegatingAvailabilityProbesHealthEndpointGroup group;

	private HttpCodeStatusMapper mapper;

	private StatusAggregator aggregator;

	@BeforeEach
	void setup() {
		HealthEndpointGroup delegate = mock(HealthEndpointGroup.class);
		this.mapper = mock(HttpCodeStatusMapper.class);
		this.aggregator = mock(StatusAggregator.class);
		given(delegate.getHttpCodeStatusMapper()).willReturn(this.mapper);
		given(delegate.getStatusAggregator()).willReturn(this.aggregator);
		given(delegate.showComponents(any())).willReturn(true);
		given(delegate.showDetails(any())).willReturn(false);
		given(delegate.isMember("test")).willReturn(true);
		this.group = new DelegatingAvailabilityProbesHealthEndpointGroup(delegate,
				AdditionalHealthEndpointPath.from("server:test"));
	}

	@Test
	void groupDelegatesToDelegate() {
		assertThat(this.group.getHttpCodeStatusMapper()).isEqualTo(this.mapper);
		assertThat(this.group.getStatusAggregator()).isEqualTo(this.aggregator);
		assertThat(this.group.isMember("test")).isTrue();
		assertThat(this.group.showDetails(null)).isFalse();
		assertThat(this.group.showComponents(null)).isTrue();
		assertThat(this.group.getAdditionalPath().getValue()).isEqualTo("test");
	}

}
