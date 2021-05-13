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

package org.springframework.boot.autoconfigure.batch;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.configuration.annotation.BatchConfigurer;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link BatchAutoConfiguration} when Spring JDBC is not on the classpath.
 *
 * @author Andy Wilkinson
 */
@ClassPathExclusions("spring-jdbc-*.jar")
class BatchAutoConfigurationWithoutJdbcTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(BatchAutoConfiguration.class, TransactionAutoConfiguration.class))
			.withUserConfiguration(BatchConfiguration.class);

	@Test
	void whenThereIsNoJdbcOnTheClasspathThenComponentsAreStillAutoConfigured() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(JobLauncherApplicationRunner.class);
			assertThat(context).hasSingleBean(JobExecutionExitCodeGenerator.class);
			assertThat(context).hasSingleBean(SimpleJobOperator.class);
		});
	}

	@Configuration
	@EnableBatchProcessing
	static class BatchConfiguration implements BatchConfigurer {

		@Override
		public JobRepository getJobRepository() {
			return mock(JobRepository.class);
		}

		@Override
		public PlatformTransactionManager getTransactionManager() {
			return mock(PlatformTransactionManager.class);
		}

		@Override
		public JobLauncher getJobLauncher() {
			return mock(JobLauncher.class);
		}

		@Override
		public JobExplorer getJobExplorer() {
			return mock(JobExplorer.class);
		}

	}

}
