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

package org.springframework.boot.docker.compose.lifecycle;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LifecycleManagement}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class LifecycleManagementTests {

	@Test
	void shouldStartupWhenNone() {
		assertThat(LifecycleManagement.NONE.shouldStart()).isFalse();
	}

	@Test
	void shouldShutdownWhenNone() {
		assertThat(LifecycleManagement.NONE.shouldStop()).isFalse();
	}

	@Test
	void shouldStartupWhenStartOnly() {
		assertThat(LifecycleManagement.START_ONLY.shouldStart()).isTrue();
	}

	@Test
	void shouldShutdownWhenStartOnly() {
		assertThat(LifecycleManagement.START_ONLY.shouldStop()).isFalse();
	}

	@Test
	void shouldStartupWhenStartAndStop() {
		assertThat(LifecycleManagement.START_AND_STOP.shouldStart()).isTrue();
	}

	@Test
	void shouldShutdownWhenStartAndStop() {
		assertThat(LifecycleManagement.START_AND_STOP.shouldStop()).isTrue();
	}

}
