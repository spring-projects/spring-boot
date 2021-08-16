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

package org.springframework.boot.actuate.autoconfigure.system;

import java.io.File;
import java.util.Map;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.health.HealthContributorAutoConfiguration;
import org.springframework.boot.actuate.system.DiskSpaceHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link DiskSpaceHealthContributorAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Chris Bono
 */
class DiskSpaceHealthContributorAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(DiskSpaceHealthContributorAutoConfiguration.class,
					HealthContributorAutoConfiguration.class));

	@Test
	void runShouldCreateIndicatorWithDefaultSinglePathAndThreshold() {
		this.contextRunner
				.run((context) -> validateIndicatorHasPathsExactly(entry(new File("."), DataSize.ofMegabytes(10))));
	}

	@Test
	void pathCanBeCustomized() {
		this.contextRunner.withPropertyValues("management.health.diskspace.paths[0].path=..")
				.run((context) -> validateIndicatorHasPathsExactly(entry(new File(".."), DataSize.ofMegabytes(10))));
	}

	@Test
	void thresholdCanBeCustomized() {
		this.contextRunner.withPropertyValues("management.health.diskspace.paths[0].threshold=20MB")
				.run((context) -> validateIndicatorHasPathsExactly(entry(new File("."), DataSize.ofMegabytes(20))));
	}

	@Test
	void pathAndThresholdCanBeCustomized() {
		this.contextRunner
				.withPropertyValues("management.health.diskspace.paths[0].path=..",
						"management.health.diskspace.paths[0].threshold=20MB")
				.run((context) -> validateIndicatorHasPathsExactly(entry(new File(".."), DataSize.ofMegabytes(20))));
	}

	@Test
	void multiplePathsCanBeConfigured() {
		this.contextRunner.withPropertyValues("management.health.diskspace.paths[0].path=.",
				"management.health.diskspace.paths[1].path=..", "management.health.diskspace.paths[1].threshold=33MB")
				.run((context) -> validateIndicatorHasPathsExactly(entry(new File("."), DataSize.ofMegabytes(10)),
						entry(new File(".."), DataSize.ofMegabytes(33))));
	}

	@Test
	void thresholdMustBePositive() {
		this.contextRunner.withPropertyValues("management.health.diskspace.paths[0].threshold=-10MB")
				.run((context) -> assertThat(context).hasFailed().getFailure().getCause().hasMessageContaining(
						"Failed to bind properties under 'management.health.diskspace.paths[0]'"));
	}

	@Test
	void runWhenPathDoesNotExistShouldCreateIndicator() {
		this.contextRunner.withPropertyValues("management.health.diskspace.paths[0].path=does/not/exist")
				.run((context) -> assertThat(context).hasSingleBean(DiskSpaceHealthIndicator.class));
	}

	@Test
	void runWhenDisabledShouldNotCreateIndicator() {
		this.contextRunner.withPropertyValues("management.health.diskspace.enabled:false")
				.run((context) -> assertThat(context).doesNotHaveBean(DiskSpaceHealthIndicator.class));
	}

	@SafeVarargs
	@SuppressWarnings("varargs")
	private final ContextConsumer<AssertableApplicationContext> validateIndicatorHasPathsExactly(
			Map.Entry<File, DataSize>... entries) {
		return (context -> assertThat(context).hasSingleBean(DiskSpaceHealthIndicator.class)
				.getBean(DiskSpaceHealthIndicator.class).extracting("paths")
				.asInstanceOf(InstanceOfAssertFactories.map(File.class, DataSize.class)).containsExactly(entries));
	}

}
