/*
 * Copyright 2012-2023 the original author or authors.
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

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.health.AdditionalHealthEndpointPath;
import org.springframework.boot.actuate.health.HealthEndpointGroup;
import org.springframework.boot.actuate.health.HealthEndpointGroups;
import org.springframework.boot.actuate.health.HttpCodeStatusMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AvailabilityProbesHealthEndpointGroups}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class AvailabilityProbesHealthEndpointGroupsTests {

	private HealthEndpointGroups delegate;

	private HealthEndpointGroup group;

	@BeforeEach
	void setup() {
		this.delegate = mock(HealthEndpointGroups.class);
		this.group = mock(HealthEndpointGroup.class);
	}

	@Test
	void createWhenGroupsIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new AvailabilityProbesHealthEndpointGroups(null, false))
			.withMessage("Groups must not be null");
	}

	@Test
	void getPrimaryDelegatesToGroups() {
		given(this.delegate.getPrimary()).willReturn(this.group);
		HealthEndpointGroups availabilityProbes = new AvailabilityProbesHealthEndpointGroups(this.delegate, false);
		assertThat(availabilityProbes.getPrimary()).isEqualTo(this.group);
	}

	@Test
	void getNamesIncludesAvailabilityProbeGroups() {
		given(this.delegate.getNames()).willReturn(Collections.singleton("test"));
		HealthEndpointGroups availabilityProbes = new AvailabilityProbesHealthEndpointGroups(this.delegate, false);
		assertThat(availabilityProbes.getNames()).containsExactly("test", "liveness", "readiness");
	}

	@Test
	void getWhenProbeInDelegateReturnsOriginalGroup() {
		HealthEndpointGroup group = mock(HealthEndpointGroup.class);
		HttpCodeStatusMapper mapper = mock(HttpCodeStatusMapper.class);
		given(group.getHttpCodeStatusMapper()).willReturn(mapper);
		given(this.delegate.get("liveness")).willReturn(group);
		HealthEndpointGroups availabilityProbes = new AvailabilityProbesHealthEndpointGroups(this.delegate, false);
		assertThat(availabilityProbes.get("liveness")).isEqualTo(group);
		assertThat(group.getHttpCodeStatusMapper()).isEqualTo(mapper);
	}

	@Test
	void getWhenProbeInDelegateAndExistingAdditionalPathReturnsOriginalGroup() {
		HealthEndpointGroup group = mock(HealthEndpointGroup.class);
		given(group.getAdditionalPath()).willReturn(AdditionalHealthEndpointPath.from("server:test"));
		given(this.delegate.get("liveness")).willReturn(group);
		HealthEndpointGroups availabilityProbes = new AvailabilityProbesHealthEndpointGroups(this.delegate, true);
		HealthEndpointGroup liveness = availabilityProbes.get("liveness");
		assertThat(liveness).isEqualTo(group);
		assertThat(liveness.getAdditionalPath().getValue()).isEqualTo("test");
	}

	@Test
	void getWhenProbeInDelegateAndAdditionalPathReturnsGroupWithAdditionalPath() {
		given(this.delegate.get("liveness")).willReturn(this.group);
		HealthEndpointGroups availabilityProbes = new AvailabilityProbesHealthEndpointGroups(this.delegate, true);
		assertThat(availabilityProbes.get("liveness").getAdditionalPath().getValue()).isEqualTo("/livez");
	}

	@Test
	void getWhenProbeNotInDelegateReturnsProbeGroup() {
		HealthEndpointGroups availabilityProbes = new AvailabilityProbesHealthEndpointGroups(this.delegate, false);
		assertThat(availabilityProbes.get("liveness")).isInstanceOf(AvailabilityProbesHealthEndpointGroup.class);
	}

	@Test
	void getWhenNotProbeAndNotInDelegateReturnsNull() {
		HealthEndpointGroups availabilityProbes = new AvailabilityProbesHealthEndpointGroups(this.delegate, false);
		assertThat(availabilityProbes.get("mygroup")).isNull();
	}

	@Test
	void getLivenessProbeHasOnlyLivenessStateAsMember() {
		HealthEndpointGroups availabilityProbes = new AvailabilityProbesHealthEndpointGroups(this.delegate, false);
		HealthEndpointGroup probeGroup = availabilityProbes.get("liveness");
		assertThat(probeGroup.isMember("livenessState")).isTrue();
		assertThat(probeGroup.isMember("readinessState")).isFalse();
	}

	@Test
	void getReadinessProbeHasOnlyReadinessStateAsMember() {
		HealthEndpointGroups availabilityProbes = new AvailabilityProbesHealthEndpointGroups(this.delegate, false);
		HealthEndpointGroup probeGroup = availabilityProbes.get("readiness");
		assertThat(probeGroup.isMember("livenessState")).isFalse();
		assertThat(probeGroup.isMember("readinessState")).isTrue();
	}

}
