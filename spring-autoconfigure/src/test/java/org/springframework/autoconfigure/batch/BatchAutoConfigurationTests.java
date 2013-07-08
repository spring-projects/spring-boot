/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.autoconfigure.batch;

import java.util.Collection;
import java.util.Collections;

import org.junit.Test;
import org.springframework.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.autoconfigure.batch.JobLauncherCommandLineRunner;
import org.springframework.autoconfigure.jdbc.EmbeddedDatabaseConfiguration;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.AbstractJob;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;

import static org.junit.Assert.assertNotNull;

/**
 * Tests for {@link BatchAutoConfiguration}.
 * 
 * @author Dave Syer
 */
public class BatchAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@Test
	public void testDefaultContext() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(TestConfiguration.class, BatchAutoConfiguration.class,
				EmbeddedDatabaseConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(JobLauncher.class));
	}

	@Test
	public void testDefinesAndLaunchesJob() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(JobConfiguration.class, BatchAutoConfiguration.class,
				EmbeddedDatabaseConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(JobLauncher.class));
		this.context.getBean(JobLauncherCommandLineRunner.class).run();
		assertNotNull(this.context.getBean(JobRepository.class).getLastJobExecution(
				"job", new JobParameters()));
	}

	@EnableBatchProcessing
	protected static class TestConfiguration {
	}

	@EnableBatchProcessing
	protected static class JobConfiguration {
		@Autowired
		private JobRepository jobRepository;

		@Bean
		public Job job() {
			AbstractJob job = new AbstractJob() {

				@Override
				public Collection<String> getStepNames() {
					return Collections.emptySet();
				}

				@Override
				public Step getStep(String stepName) {
					return null;
				}

				@Override
				protected void doExecute(JobExecution execution)
						throws JobExecutionException {
					execution.setStatus(BatchStatus.COMPLETED);
				}
			};
			job.setJobRepository(this.jobRepository);
			return job;
		}
	}

}
