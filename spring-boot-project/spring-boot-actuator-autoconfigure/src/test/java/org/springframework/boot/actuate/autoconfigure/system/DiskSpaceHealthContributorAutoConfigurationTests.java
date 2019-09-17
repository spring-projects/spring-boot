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

package org.springframework.boot.actuate.autoconfigure.system;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.health.HealthContributorAutoConfiguration;
import org.springframework.boot.actuate.system.DiskSpaceHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DiskSpaceHealthContributorAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class DiskSpaceHealthContributorAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(AutoConfigurations
			.of(DiskSpaceHealthContributorAutoConfiguration.class, HealthContributorAutoConfiguration.class));

	@Test
	void runShouldCreateIndicator() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(DiskSpaceHealthIndicator.class));
	}

	@Test
	void thresholdMustBePositive() {
		this.contextRunner.withPropertyValues("management.health.diskspace.threshold=-10MB")
				.run((context) -> assertThat(context).hasFailed().getFailure()
						.hasMessageContaining("Failed to bind properties under 'management.health.diskspace'"));
	}

	@Test
	void thresholdCanBeCustomized() {
		this.contextRunner.withPropertyValues("management.health.diskspace.threshold=20MB").run((context) -> {
			assertThat(context).hasSingleBean(DiskSpaceHealthIndicator.class);
			assertThat(context.getBean(DiskSpaceHealthIndicator.class)).hasFieldOrPropertyWithValue("threshold",
					DataSize.ofMegabytes(20));
		});
	}

	@Test
	void runWhenDisabledShouldNotCreateIndicator() {
		this.contextRunner.withPropertyValues("management.health.diskspace.enabled:false")
				.run((context) -> assertThat(context).doesNotHaveBean(DiskSpaceHealthIndicator.class));
	}

}
