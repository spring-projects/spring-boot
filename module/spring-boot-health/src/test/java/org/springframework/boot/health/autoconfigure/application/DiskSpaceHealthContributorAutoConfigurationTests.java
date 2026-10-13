/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.autoconfigure.application;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.health.application.DiskSpaceHealthIndicator;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
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

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(DiskSpaceHealthContributorAutoConfiguration.class,
				HealthContributorAutoConfiguration.class));

	@Test
	void runShouldCreateIndicator() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(DiskSpaceHealthIndicator.class));
	}

	@Test
	void thresholdMustBePositive() {
		this.contextRunner.withPropertyValues("management.health.diskspace.threshold=-10MB")
			.run((context) -> assertThat(context).hasFailed()
				.getFailure()
				.rootCause()
				.hasMessage("'threshold' must be greater than or equal to 0"));
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
	void runWhenPathDoesNotExistShouldCreateIndicator() {
		this.contextRunner.withPropertyValues("management.health.diskspace.path=does/not/exist")
			.run((context) -> assertThat(context).hasSingleBean(DiskSpaceHealthIndicator.class));
	}

	@Test
	void runWhenDisabledShouldNotCreateIndicator() {
		this.contextRunner.withPropertyValues("management.health.diskspace.enabled:false")
			.run((context) -> assertThat(context).doesNotHaveBean(DiskSpaceHealthIndicator.class));
	}

}
