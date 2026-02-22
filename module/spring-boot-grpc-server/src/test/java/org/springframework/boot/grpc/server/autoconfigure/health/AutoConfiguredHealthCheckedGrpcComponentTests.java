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

package org.springframework.boot.grpc.server.autoconfigure.health;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.grpc.server.health.StatusAggregator;
import org.springframework.boot.grpc.server.health.StatusMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AutoConfiguredHealthCheckedGrpcComponent}.
 *
 * @author Phillip Webb
 */
@ExtendWith(MockitoExtension.class)
class AutoConfiguredHealthCheckedGrpcComponentTests {

	@Mock
	@SuppressWarnings("NullAway.Init")
	private StatusAggregator statusAggregator;

	@Mock
	@SuppressWarnings("NullAway.Init")
	private StatusMapper statusMapper;

	@Test
	void isMemberWhenMembershipMatchesAcceptsTrue() {
		AutoConfiguredHealthCheckedGrpcComponent component = new AutoConfiguredHealthCheckedGrpcComponent(
				(name) -> name.startsWith("a"), this.statusAggregator, this.statusMapper);
		assertThat(component.isMember("albert")).isTrue();
		assertThat(component.isMember("arnold")).isTrue();
	}

	@Test
	void isMemberWhenMembershipRejectsReturnsTrue() {
		AutoConfiguredHealthCheckedGrpcComponent component = new AutoConfiguredHealthCheckedGrpcComponent(
				(name) -> name.startsWith("a"), this.statusAggregator, this.statusMapper);
		assertThat(component.isMember("bert")).isFalse();
		assertThat(component.isMember("ernie")).isFalse();
	}

	@Test
	void getStatusAggregatorReturnsStatusAggregator() {
		AutoConfiguredHealthCheckedGrpcComponent component = new AutoConfiguredHealthCheckedGrpcComponent(
				(name) -> true, this.statusAggregator, this.statusMapper);
		assertThat(component.getStatusAggregator()).isSameAs(this.statusAggregator);
	}

	@Test
	void getStatusMapperReturnsHttpCodeStatusMapper() {
		AutoConfiguredHealthCheckedGrpcComponent component = new AutoConfiguredHealthCheckedGrpcComponent(
				(name) -> true, this.statusAggregator, this.statusMapper);
		assertThat(component.getStatusMapper()).isSameAs(this.statusMapper);
	}

}
