/*
 * Copyright 2012-2021 the original author or authors.
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

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.health.HealthEndpointGroup;
import org.springframework.boot.actuate.health.HealthEndpointGroups;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AvailabilityProbesHealthEndpointGroupsPostProcessor}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class AvailabilityProbesHealthEndpointGroupsPostProcessorTests {

	private AvailabilityProbesHealthEndpointGroupsPostProcessor postProcessor = new AvailabilityProbesHealthEndpointGroupsPostProcessor(
			new MockEnvironment());

	@Test
	void postProcessHealthEndpointGroupsWhenGroupsAlreadyContainedReturnsOriginal() {
		HealthEndpointGroups groups = mock(HealthEndpointGroups.class);
		Set<String> names = new LinkedHashSet<>();
		names.add("test");
		names.add("readiness");
		names.add("liveness");
		given(groups.getNames()).willReturn(names);
		assertThat(this.postProcessor.postProcessHealthEndpointGroups(groups)).isSameAs(groups);
	}

	@Test
	void postProcessHealthEndpointGroupsWhenGroupContainsOneReturnsPostProcessed() {
		HealthEndpointGroups groups = mock(HealthEndpointGroups.class);
		Set<String> names = new LinkedHashSet<>();
		names.add("test");
		names.add("readiness");
		given(groups.getNames()).willReturn(names);
		assertThat(this.postProcessor.postProcessHealthEndpointGroups(groups))
				.isInstanceOf(AvailabilityProbesHealthEndpointGroups.class);
	}

	@Test
	void postProcessHealthEndpointGroupsWhenGroupsContainsNoneReturnsProcessed() {
		HealthEndpointGroups groups = mock(HealthEndpointGroups.class);
		Set<String> names = new LinkedHashSet<>();
		names.add("test");
		names.add("spring");
		names.add("boot");
		given(groups.getNames()).willReturn(names);
		assertThat(this.postProcessor.postProcessHealthEndpointGroups(groups))
				.isInstanceOf(AvailabilityProbesHealthEndpointGroups.class);
	}

	@Test
	void postProcessHealthEndpointGroupsWhenAdditionalPathPropertyIsTrue() {
		HealthEndpointGroups postProcessed = getPostProcessed("true");
		HealthEndpointGroup liveness = postProcessed.get("liveness");
		HealthEndpointGroup readiness = postProcessed.get("readiness");
		assertThat(liveness.getAdditionalPath().toString()).isEqualTo("server:/livez");
		assertThat(readiness.getAdditionalPath().toString()).isEqualTo("server:/readyz");
	}

	private HealthEndpointGroups getPostProcessed(String value) {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("management.endpoint.health.probes.add-additional-paths", value);
		AvailabilityProbesHealthEndpointGroupsPostProcessor postProcessor = new AvailabilityProbesHealthEndpointGroupsPostProcessor(
				environment);
		HealthEndpointGroups groups = mock(HealthEndpointGroups.class);
		return postProcessor.postProcessHealthEndpointGroups(groups);
	}

	@Test
	void postProcessHealthEndpointGroupsWhenAdditionalPathPropertyIsFalse() {
		HealthEndpointGroups postProcessed = getPostProcessed("false");
		HealthEndpointGroup liveness = postProcessed.get("liveness");
		HealthEndpointGroup readiness = postProcessed.get("readiness");
		assertThat(liveness.getAdditionalPath()).isNull();
		assertThat(readiness.getAdditionalPath()).isNull();
	}

	@Test
	void postProcessHealthEndpointGroupsWhenAdditionalPathPropertyIsNull() {
		HealthEndpointGroups groups = mock(HealthEndpointGroups.class);
		HealthEndpointGroups postProcessed = this.postProcessor.postProcessHealthEndpointGroups(groups);
		HealthEndpointGroup liveness = postProcessed.get("liveness");
		HealthEndpointGroup readiness = postProcessed.get("readiness");
		assertThat(liveness.getAdditionalPath()).isNull();
		assertThat(readiness.getAdditionalPath()).isNull();
	}

}
