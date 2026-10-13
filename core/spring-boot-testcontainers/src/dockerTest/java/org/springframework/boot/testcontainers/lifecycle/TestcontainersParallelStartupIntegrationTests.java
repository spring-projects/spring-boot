/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testcontainers.lifecycle;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.postgresql.PostgreSQLContainer;

import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.lifecycle.TestcontainersParallelStartupIntegrationTests.ContainerConfig;
import org.springframework.boot.testsupport.container.DisabledIfDockerUnavailable;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for parallel startup.
 *
 * @author Phillip Webb
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ContainerConfig.class)
@TestPropertySource(properties = "spring.testcontainers.beans.startup=parallel")
@DisabledIfDockerUnavailable
@ExtendWith(OutputCaptureExtension.class)
class TestcontainersParallelStartupIntegrationTests {

	@Test
	void startsInParallel(CapturedOutput out) {
		assertThat(out).contains("-lifecycle-0").contains("-lifecycle-1").contains("-lifecycle-2");
	}

	@Configuration(proxyBeanMethods = false)
	static class ContainerConfig {

		@Bean
		static PostgreSQLContainer container1() {
			return TestImage.forContainer(PostgreSQLContainer.class);
		}

		@Bean
		static PostgreSQLContainer container2() {
			return TestImage.forContainer(PostgreSQLContainer.class);
		}

		@Bean
		static PostgreSQLContainer container3() {
			return TestImage.forContainer(PostgreSQLContainer.class);
		}

	}

}
