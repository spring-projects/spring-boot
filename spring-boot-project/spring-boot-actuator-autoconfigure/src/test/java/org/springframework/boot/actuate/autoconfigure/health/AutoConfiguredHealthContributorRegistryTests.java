/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.health;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.HealthContributorRegistry;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AutoConfiguredHealthContributorRegistry}.
 *
 * @author Phillip Webb
 */
class AutoConfiguredHealthContributorRegistryTests {

	@Test
	void createWhenContributorsClashesWithGroupNameThrowsException() {
		assertThatIllegalStateException()
				.isThrownBy(() -> new AutoConfiguredHealthContributorRegistry(
						Collections.singletonMap("boot", mock(HealthContributor.class)),
						Arrays.asList("spring", "boot")))
				.withMessage("HealthContributor with name \"boot\" clashes with group");
	}

	@Test
	void registerContributorWithGroupNameThrowsException() {
		HealthContributorRegistry registry = new AutoConfiguredHealthContributorRegistry(Collections.emptyMap(),
				Arrays.asList("spring", "boot"));
		assertThatIllegalStateException()
				.isThrownBy(() -> registry.registerContributor("spring", mock(HealthContributor.class)))
				.withMessage("HealthContributor with name \"spring\" clashes with group");
	}

}
