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

package org.springframework.boot.buildpack.platform.docker.type;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ContainerStatus}.
 *
 * @author Scott Frederick
 */
class ContainerStatusTests {

	@Test
	void ofCreatesFromJson() throws IOException {
		ContainerStatus status = ContainerStatus.of(getClass().getResourceAsStream("container-status-error.json"));
		assertThat(status.getStatusCode()).isEqualTo(1);
		assertThat(status.getWaitingErrorMessage()).isEqualTo("error detail");
	}

	@Test
	void ofCreatesFromValues() {
		ContainerStatus status = ContainerStatus.of(1, "error detail");
		assertThat(status.getStatusCode()).isEqualTo(1);
		assertThat(status.getWaitingErrorMessage()).isEqualTo("error detail");
	}

}
