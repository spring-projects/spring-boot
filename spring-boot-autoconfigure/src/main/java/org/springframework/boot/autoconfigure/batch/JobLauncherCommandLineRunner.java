/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.batch;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersIncrementer;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobParametersNotFoundException;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Component;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

/**
 * {@link CommandLineRunner} to {@link JobLauncher launch} Spring Batch jobs. Runs all
 * jobs in the surrounding context by default. Can also be used to launch a specific job
 * by providing a jobName
 * 
 * @author Dave Syer
 */
@Component
public class JobLauncherCommandLineRunner implements CommandLineRunner,
		ApplicationEventPublisherAware {

	private static Log logger = LogFactory.getLog(JobLauncherCommandLineRunner.class);

	private JobParametersConverter converter = new DefaultJobParametersConverter();

	private JobLauncher jobLauncher;

	private JobRegistry jobRegistry;

	private JobExplorer jobExplorer;

	private String jobNames;

	private Collection<Job> jobs = Collections.emptySet();

	private ApplicationEventPublisher publisher;

	public JobLauncherCommandLineRunner(JobLauncher jobLauncher, JobExplorer jobExplorer) {
		this.jobLauncher = jobLauncher;
		this.jobExplorer = jobExplorer;
	}

	public void setJobNames(String jobNames) {
		this.jobNames = jobNames;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	@Autowired(required = false)
	public void setJobRegistry(JobRegistry jobRegistry) {
		this.jobRegistry = jobRegistry;
	}

	@Autowired(required = false)
	public void setJobParametersConverter(JobParametersConverter converter) {
		this.converter = converter;
	}

	@Autowired(required = false)
	public void setJobs(Collection<Job> jobs) {
		this.jobs = jobs;
	}

	@Override
	public void run(String... args) throws JobExecutionException {
		logger.info("Running default command line with: " + Arrays.asList(args));
		launchJobFromProperties(StringUtils.splitArrayElementsIntoProperties(args, "="));
	}

	protected void launchJobFromProperties(Properties properties)
			throws JobExecutionException {
		JobParameters jobParameters = this.converter.getJobParameters(properties);
		executeLocalJobs(jobParameters);
		executeRegisteredJobs(jobParameters);
	}

	private JobParameters getNextJobParameters(Job job, JobParameters additionalParameters) {

		String jobIdentifier = job.getName();
		JobParameters jobParameters = new JobParameters();
		List<JobInstance> lastInstances = this.jobExplorer.getJobInstances(jobIdentifier,
				0, 1);

		JobParametersIncrementer incrementer = job.getJobParametersIncrementer();

		Map<String, JobParameter> additionals = additionalParameters.getParameters();
		if (lastInstances.isEmpty()) {
			// Start from a completely clean sheet
			if (incrementer != null) {
				jobParameters = incrementer.getNext(new JobParameters());
			}
		}
		else {
			List<JobExecution> lastExecutions = this.jobExplorer
					.getJobExecutions(lastInstances.get(0));
			JobExecution previousExecution = lastExecutions.get(0);
			if (previousExecution == null) {
				// Normally this will not happen - an instance exists with no executions
				if (incrementer != null) {
					jobParameters = incrementer.getNext(new JobParameters());
				}
			}
			else if (previousExecution.getStatus() == BatchStatus.STOPPED
					|| previousExecution.getStatus() == BatchStatus.FAILED) {
				// Retry a failed or stopped execution
				jobParameters = previousExecution.getJobParameters();
				for (Entry<String, JobParameter> parameter : additionals.entrySet()) {
					// Non-identifying additional parameters can be added to a retry
					if (!parameter.getValue().isIdentifying()) {
						additionals.remove(parameter.getKey());
					}
				}
			}
			else if (incrementer != null) {
				// New instance so increment the parameters if we can
				if (incrementer != null) {
					jobParameters = incrementer.getNext(previousExecution
							.getJobParameters());
				}
			}
		}

		Map<String, JobParameter> map = new HashMap<String, JobParameter>(
				jobParameters.getParameters());
		map.putAll(additionals);
		jobParameters = new JobParameters(map);

		return jobParameters;

	}

	private void executeRegisteredJobs(JobParameters jobParameters)
			throws JobExecutionException {
		if (this.jobRegistry != null && StringUtils.hasText(this.jobNames)) {
			String[] jobsToRun = this.jobNames.split(",");
			for (String jobName : jobsToRun) {
				try {
					Job job = this.jobRegistry.getJob(jobName);
					if (this.jobs.contains(job)) {
						continue;
					}
					execute(job, jobParameters);
				}
				catch (NoSuchJobException nsje) {
					logger.debug("No job found in registry for job name: " + jobName);
					continue;
				}
			}
		}
	}

	protected void execute(Job job, JobParameters jobParameters)
			throws JobExecutionAlreadyRunningException, JobRestartException,
			JobInstanceAlreadyCompleteException, JobParametersInvalidException,
			JobParametersNotFoundException {
		JobParameters nextParameters = getNextJobParameters(job, jobParameters);
		if (nextParameters != null) {
			JobExecution execution = this.jobLauncher.run(job, nextParameters);
			if (this.publisher != null) {
				this.publisher.publishEvent(new JobExecutionEvent(execution));
			}
		}
	}

	private void executeLocalJobs(JobParameters jobParameters)
			throws JobExecutionException {
		for (Job job : this.jobs) {
			if (StringUtils.hasText(this.jobNames)) {
				String[] jobsToRun = this.jobNames.split(",");
				if (!PatternMatchUtils.simpleMatch(jobsToRun, job.getName())) {
					logger.debug("Skipped job: " + job.getName());
					continue;
				}
			}
			execute(job, jobParameters);
		}
	}

}
