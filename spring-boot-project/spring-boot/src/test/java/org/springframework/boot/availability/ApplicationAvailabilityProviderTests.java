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

package org.springframework.boot.availability;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ApplicationAvailabilityProvider}
 *
 * @author Brian Clozel
 */
class ApplicationAvailabilityProviderTests {

	@Test
	void initialStateShouldBeFailures() {
		ApplicationAvailabilityProvider stateProvider = new ApplicationAvailabilityProvider();
		assertThat(stateProvider.getLivenessState()).isEqualTo(LivenessState.broken());
		assertThat(stateProvider.getReadinessState()).isEqualTo(ReadinessState.unready());
	}

	@Test
	void updateLivenessState() {
		ApplicationAvailabilityProvider stateProvider = new ApplicationAvailabilityProvider();
		LivenessState livenessState = LivenessState.live();
		stateProvider.onApplicationEvent(new LivenessStateChangedEvent(livenessState, "Startup complete"));
		assertThat(stateProvider.getLivenessState()).isEqualTo(livenessState);
	}

	@Test
	void updateReadiessState() {
		ApplicationAvailabilityProvider stateProvider = new ApplicationAvailabilityProvider();
		stateProvider.onApplicationEvent(ReadinessStateChangedEvent.ready());
		assertThat(stateProvider.getReadinessState()).isEqualTo(ReadinessState.ready());
	}

}
