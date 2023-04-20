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

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.health.HttpCodeStatusMapper;
import org.springframework.boot.actuate.health.StatusAggregator;

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
		assertThat(this.group.getHttpCodeStatusMapper()).isEqualTo(HttpCodeStatusMapper.DEFAULT);
	}

}
