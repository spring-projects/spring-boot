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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.health.actuate.endpoint.AdditionalHealthEndpointPath;
import org.springframework.boot.health.actuate.endpoint.HealthEndpointGroup;
import org.springframework.boot.health.actuate.endpoint.HealthEndpointGroups;

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
	@SuppressWarnings("NullAway") // Test null check
	void createWhenGroupsIsNullThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new AvailabilityProbesHealthEndpointGroups(null, false, new HealthEndpointProperties()))
			.withMessage("'groups' must not be null");
	}

	@Test
	void getPrimaryDelegatesToGroups() {
		given(this.delegate.getPrimary()).willReturn(this.group);
		HealthEndpointGroups availabilityProbes = new AvailabilityProbesHealthEndpointGroups(this.delegate, false,
				new HealthEndpointProperties());
		assertThat(availabilityProbes.getPrimary()).isEqualTo(this.group);
	}

	@Test
	void getNamesIncludesAvailabilityProbeGroups() {
		given(this.delegate.getNames()).willReturn(Collections.singleton("test"));
		HealthEndpointGroups availabilityProbes = new AvailabilityProbesHealthEndpointGroups(this.delegate, false,
				new HealthEndpointProperties());
		assertThat(availabilityProbes.getNames()).containsExactly("test", "liveness", "readiness");
	}

	@Test
	void getWhenProbeInDelegateWithExplicitIncludeReturnsOriginalGroup() {
		HealthEndpointGroup group = mock(HealthEndpointGroup.class);
		given(this.delegate.get("liveness")).willReturn(group);
		HealthEndpointGroups availabilityProbes = new AvailabilityProbesHealthEndpointGroups(this.delegate, false,
				propertiesWithInclude("liveness", "livenessState", "diskSpace"));
		assertThat(availabilityProbes.get("liveness")).isEqualTo(group);
	}

	@Test
	void getWhenProbeInDelegateWithNoExplicitMembershipRetainsProbeDefaults() {
		given(this.delegate.get("liveness")).willReturn(this.group);
		HealthEndpointGroups availabilityProbes = new AvailabilityProbesHealthEndpointGroups(this.delegate, false,
				new HealthEndpointProperties());
		HealthEndpointGroup liveness = availabilityProbes.get("liveness");
		assertThat(liveness).isNotNull();
		assertThat(liveness.isMember("livenessState")).isTrue();
		assertThat(liveness.isMember("diskSpace")).isFalse();
	}

	@Test
	void getWhenProbeInDelegateWithPropertiesGroupButNoMembershipRetainsProbeDefaults() {
		given(this.delegate.get("liveness")).willReturn(this.group);
		HealthEndpointProperties.Group propertiesGroup = new HealthEndpointProperties.Group();
		propertiesGroup.setAdditionalPath("server:/custom");
		HealthEndpointProperties properties = new HealthEndpointProperties();
		properties.getGroup().put("liveness", propertiesGroup);
		HealthEndpointGroups availabilityProbes = new AvailabilityProbesHealthEndpointGroups(this.delegate, false,
				properties);
		HealthEndpointGroup liveness = availabilityProbes.get("liveness");
		assertThat(liveness).isNotNull();
		assertThat(liveness.isMember("livenessState")).isTrue();
		assertThat(liveness.isMember("diskSpace")).isFalse();
	}

	@Test
	void getWhenReadinessProbeInDelegateWithNoExplicitMembershipRetainsProbeDefaults() {
		given(this.delegate.get("readiness")).willReturn(this.group);
		HealthEndpointGroups availabilityProbes = new AvailabilityProbesHealthEndpointGroups(this.delegate, false,
				new HealthEndpointProperties());
		HealthEndpointGroup readiness = availabilityProbes.get("readiness");
		assertThat(readiness).isNotNull();
		assertThat(readiness.isMember("readinessState")).isTrue();
		assertThat(readiness.isMember("diskSpace")).isFalse();
	}

	@Test
	void getWhenProbeInDelegateAndExistingAdditionalPathPreservesPathWithProbeDefaults() {
		HealthEndpointGroup group = mock(HealthEndpointGroup.class);
		given(group.getAdditionalPath()).willReturn(AdditionalHealthEndpointPath.from("server:test"));
		given(this.delegate.get("liveness")).willReturn(group);
		HealthEndpointGroups availabilityProbes = new AvailabilityProbesHealthEndpointGroups(this.delegate, true,
				new HealthEndpointProperties());
		HealthEndpointGroup liveness = availabilityProbes.get("liveness");
		assertThat(liveness).isNotNull();
		assertThat(liveness.isMember("livenessState")).isTrue();
		assertThat(liveness.isMember("diskSpace")).isFalse();
		AdditionalHealthEndpointPath additionalPath = liveness.getAdditionalPath();
		assertThat(additionalPath).isNotNull();
		assertThat(additionalPath.getValue()).isEqualTo("test");
	}

	@Test
	void getWhenProbeInDelegateAndAdditionalPathReturnsGroupWithAdditionalPath() {
		given(this.delegate.get("liveness")).willReturn(this.group);
		HealthEndpointGroups availabilityProbes = new AvailabilityProbesHealthEndpointGroups(this.delegate, true,
				new HealthEndpointProperties());
		HealthEndpointGroup liveness = availabilityProbes.get("liveness");
		assertThat(liveness).isNotNull();
		assertThat(liveness.isMember("livenessState")).isTrue();
		assertThat(liveness.isMember("diskSpace")).isFalse();
		AdditionalHealthEndpointPath additionalPath = liveness.getAdditionalPath();
		assertThat(additionalPath).isNotNull();
		assertThat(additionalPath.getValue()).isEqualTo("/livez");
	}

	@Test
	void getWhenProbeNotInDelegateReturnsProbeGroup() {
		HealthEndpointGroups availabilityProbes = new AvailabilityProbesHealthEndpointGroups(this.delegate, false,
				new HealthEndpointProperties());
		assertThat(availabilityProbes.get("liveness")).isInstanceOf(AvailabilityProbesHealthEndpointGroup.class);
	}

	@Test
	void getWhenNotProbeAndNotInDelegateReturnsNull() {
		HealthEndpointGroups availabilityProbes = new AvailabilityProbesHealthEndpointGroups(this.delegate, false,
				new HealthEndpointProperties());
		assertThat(availabilityProbes.get("mygroup")).isNull();
	}

	@Test
	void getLivenessProbeHasOnlyLivenessStateAsMember() {
		HealthEndpointGroups availabilityProbes = new AvailabilityProbesHealthEndpointGroups(this.delegate, false,
				new HealthEndpointProperties());
		HealthEndpointGroup probeGroup = availabilityProbes.get("liveness");
		assertThat(probeGroup).isNotNull();
		assertThat(probeGroup.isMember("livenessState")).isTrue();
		assertThat(probeGroup.isMember("readinessState")).isFalse();
	}

	@Test
	void getReadinessProbeHasOnlyReadinessStateAsMember() {
		HealthEndpointGroups availabilityProbes = new AvailabilityProbesHealthEndpointGroups(this.delegate, false,
				new HealthEndpointProperties());
		HealthEndpointGroup probeGroup = availabilityProbes.get("readiness");
		assertThat(probeGroup).isNotNull();
		assertThat(probeGroup.isMember("livenessState")).isFalse();
		assertThat(probeGroup.isMember("readinessState")).isTrue();
	}

	@Test
	void getWhenProbeInDelegateWithExplicitExcludeReturnsOriginalGroup() {
		HealthEndpointGroup group = mock(HealthEndpointGroup.class);
		given(this.delegate.get("liveness")).willReturn(group);
		HealthEndpointGroups availabilityProbes = new AvailabilityProbesHealthEndpointGroups(this.delegate, false,
				propertiesWithExclude("liveness", "diskSpace"));
		assertThat(availabilityProbes.get("liveness")).isEqualTo(group);
	}

	private HealthEndpointProperties propertiesWithInclude(String probe, String... include) {
		HealthEndpointProperties properties = new HealthEndpointProperties();
		HealthEndpointProperties.Group group = new HealthEndpointProperties.Group();
		group.setInclude(new LinkedHashSet<>(Set.of(include)));
		properties.getGroup().put(probe, group);
		return properties;
	}

	private HealthEndpointProperties propertiesWithExclude(String probe, String... exclude) {
		HealthEndpointProperties properties = new HealthEndpointProperties();
		HealthEndpointProperties.Group group = new HealthEndpointProperties.Group();
		group.setExclude(new LinkedHashSet<>(Set.of(exclude)));
		properties.getGroup().put(probe, group);
		return properties;
	}

}
