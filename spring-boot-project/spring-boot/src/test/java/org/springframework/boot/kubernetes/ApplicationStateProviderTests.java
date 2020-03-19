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

package org.springframework.boot.kubernetes;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ApplicationStateProvider}
 *
 * @author Brian Clozel
 */
class ApplicationStateProviderTests {

	@Test
	void initialStateShouldBeFailures() {
		ApplicationStateProvider stateProvider = new ApplicationStateProvider();
		assertThat(stateProvider.getLivenessState()).isEqualTo(LivenessState.broken());
		assertThat(stateProvider.getReadinessState()).isEqualTo(ReadinessState.busy());
	}

	@Test
	void updateLivenessState() {
		ApplicationStateProvider stateProvider = new ApplicationStateProvider();
		LivenessState livenessState = LivenessState.live();
		stateProvider.onApplicationEvent(new LivenessStateChangedEvent(livenessState, "Startup complete"));
		assertThat(stateProvider.getLivenessState()).isEqualTo(livenessState);
	}

	@Test
	void updateReadiessState() {
		ApplicationStateProvider stateProvider = new ApplicationStateProvider();
		stateProvider.onApplicationEvent(ReadinessStateChangedEvent.ready());
		assertThat(stateProvider.getReadinessState()).isEqualTo(ReadinessState.ready());
	}

}
