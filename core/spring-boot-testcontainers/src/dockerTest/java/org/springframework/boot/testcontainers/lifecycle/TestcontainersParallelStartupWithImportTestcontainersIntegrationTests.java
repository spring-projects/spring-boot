/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testcontainers.lifecycle;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.postgresql.PostgreSQLContainer;

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
		static PostgreSQLContainer container1 = TestImage.forContainer(PostgreSQLContainer.class);

		@Container
		static PostgreSQLContainer container2 = TestImage.forContainer(PostgreSQLContainer.class);

		@Container
		static PostgreSQLContainer container3 = TestImage.forContainer(PostgreSQLContainer.class);

	}

}
