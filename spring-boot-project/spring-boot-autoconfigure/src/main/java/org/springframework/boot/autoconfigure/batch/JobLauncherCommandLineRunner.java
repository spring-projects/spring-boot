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

import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.ApplicationRunner;

/**
 * {@link ApplicationRunner} to {@link JobLauncher launch} Spring Batch jobs. Runs all
 * jobs in the surrounding context by default. Can also be used to launch a specific job
 * by providing a jobName.
 *
 * @author Dave Syer
 * @author Jean-Pierre Bergamin
 * @author Mahmoud Ben Hassine
 * @since 1.0.0
 * @deprecated since 2.3.0 for removal in 2.6.0 in favor of
 * {@link JobLauncherApplicationRunner}
 */
@Deprecated
public class JobLauncherCommandLineRunner extends JobLauncherApplicationRunner {

	/**
	 * Create a new {@link JobLauncherCommandLineRunner}.
	 * @param jobLauncher to launch jobs
	 * @param jobExplorer to check the job repository for previous executions
	 * @param jobRepository to check if a job instance exists with the given parameters
	 * when running a job
	 */
	public JobLauncherCommandLineRunner(JobLauncher jobLauncher, JobExplorer jobExplorer, JobRepository jobRepository) {
		super(jobLauncher, jobExplorer, jobRepository);
	}

}
