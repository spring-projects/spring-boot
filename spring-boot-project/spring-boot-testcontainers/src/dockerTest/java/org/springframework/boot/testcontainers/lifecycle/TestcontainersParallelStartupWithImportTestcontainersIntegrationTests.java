/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.testcontainers.lifecycle;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.boot.testcontainers.lifecycle.TestcontainersParallelStartupWithImportTestcontainersIntegrationTests.Containers;
import org.springframework.boot.testsupport.container.DisabledIfDockerUnavailable;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for parallel startup.
 *
 * @author Phillip Webb
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = "spring.testcontainers.beans.startup=parallel")
@DisabledIfDockerUnavailable
@ExtendWith({ OutputCaptureExtension.class, ResetStartablesExtension.class })
@ImportTestcontainers(Containers.class)
class TestcontainersParallelStartupWithImportTestcontainersIntegrationTests {

	@Test
	void startsInParallel(CapturedOutput out) {
		assertThat(out).contains("-lifecycle-0").contains("-lifecycle-1").contains("-lifecycle-2");
	}

	static class Containers {

		@Container
		static PostgreSQLContainer<?> container1 = TestImage.container(PostgreSQLContainer.class);

		@Container
		static PostgreSQLContainer<?> container2 = TestImage.container(PostgreSQLContainer.class);

		@Container
		static PostgreSQLContainer<?> container3 = TestImage.container(PostgreSQLContainer.class);

	}

}
