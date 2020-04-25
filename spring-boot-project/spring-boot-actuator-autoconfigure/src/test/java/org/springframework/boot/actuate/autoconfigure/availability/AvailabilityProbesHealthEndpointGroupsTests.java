/*
 * Copyright 2012-2020 the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.health.HealthEndpointGroup;
import org.springframework.boot.actuate.health.HealthEndpointGroups;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AvailabilityProbesHealthEndpointGroups}.
 *
 * @author Phillip Webb
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
		assertThatIllegalArgumentException().isThrownBy(() -> new AvailabilityProbesHealthEndpointGroups(null))
				.withMessage("Groups must not be null");
	}

	@Test
	void getPrimaryDelegatesToGroups() {
		given(this.delegate.getPrimary()).willReturn(this.group);
		HealthEndpointGroups availabilityProbes = new AvailabilityProbesHealthEndpointGroups(this.delegate);
		assertThat(availabilityProbes.getPrimary()).isEqualTo(this.group);
	}

	@Test
	void getNamesIncludesAvailabilityProbeGroups() {
		given(this.delegate.getNames()).willReturn(Collections.singleton("test"));
		HealthEndpointGroups availabilityProbes = new AvailabilityProbesHealthEndpointGroups(this.delegate);
		assertThat(availabilityProbes.getNames()).containsExactly("test", "liveness", "readiness");
	}

	@Test
	void getWhenProbeInDelegateReturnsGroupFromDelegate() {
		given(this.delegate.get("liveness")).willReturn(this.group);
		HealthEndpointGroups availabilityProbes = new AvailabilityProbesHealthEndpointGroups(this.delegate);
		assertThat(availabilityProbes.get("liveness")).isEqualTo(this.group);
	}

	@Test
	void getWhenProbeNotInDelegateReturnsProbeGroup() {
		HealthEndpointGroups availabilityProbes = new AvailabilityProbesHealthEndpointGroups(this.delegate);
		assertThat(availabilityProbes.get("liveness")).isInstanceOf(AvailabilityProbesHealthEndpointGroup.class);
	}

	@Test
	void getWhenNotProbeAndNotInDelegateReturnsNull() {
		HealthEndpointGroups availabilityProbes = new AvailabilityProbesHealthEndpointGroups(this.delegate);
		assertThat(availabilityProbes.get("mygroup")).isNull();
	}

	@Test
	void getLivenessProbeHasOnlyLivenessStateAsMember() {
		HealthEndpointGroups availabilityProbes = new AvailabilityProbesHealthEndpointGroups(this.delegate);
		HealthEndpointGroup probeGroup = availabilityProbes.get("liveness");
		assertThat(probeGroup.isMember("livenessState")).isTrue();
		assertThat(probeGroup.isMember("readinessState")).isFalse();
	}

	@Test
	void getReadinessProbeHasOnlyReadinessStateAsMember() {
		HealthEndpointGroups availabilityProbes = new AvailabilityProbesHealthEndpointGroups(this.delegate);
		HealthEndpointGroup probeGroup = availabilityProbes.get("readiness");
		assertThat(probeGroup.isMember("livenessState")).isFalse();
		assertThat(probeGroup.isMember("readinessState")).isTrue();
	}

	@Test
	void containsAllWhenContainsAllReturnTrue() {
		given(this.delegate.getNames()).willReturn(new LinkedHashSet<>(Arrays.asList("test", "liveness", "readiness")));
		assertThat(AvailabilityProbesHealthEndpointGroups.containsAllProbeGroups(this.delegate)).isTrue();
	}

	@Test
	void containsAllWhenContainsOneReturnFalse() {
		given(this.delegate.getNames()).willReturn(new LinkedHashSet<>(Arrays.asList("test", "liveness")));
		assertThat(AvailabilityProbesHealthEndpointGroups.containsAllProbeGroups(this.delegate)).isFalse();
	}

	@Test
	void containsAllWhenContainsNoneReturnFalse() {
		given(this.delegate.getNames()).willReturn(new LinkedHashSet<>(Arrays.asList("test", "spring")));
		assertThat(AvailabilityProbesHealthEndpointGroups.containsAllProbeGroups(this.delegate)).isFalse();
	}

}
