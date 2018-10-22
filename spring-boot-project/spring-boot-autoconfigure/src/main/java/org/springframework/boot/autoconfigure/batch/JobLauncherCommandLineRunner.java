/*
 * Copyright 2012-2018 the original author or authors.
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
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
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
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.Ordered;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

/**
 * {@link CommandLineRunner} to {@link JobLauncher launch} Spring Batch jobs. Runs all
 * jobs in the surrounding context by default. Can also be used to launch a specific job
 * by providing a jobName
 *
 * @author Dave Syer
 * @author Jean-Pierre Bergamin
 * @author Mahmoud Ben Hassine
 */
public class JobLauncherCommandLineRunner
		implements CommandLineRunner, Ordered, ApplicationEventPublisherAware {

	/**
	 * The default order for the command line runner.
	 */
	public static final int DEFAULT_ORDER = 0;

	private static final Log logger = LogFactory
			.getLog(JobLauncherCommandLineRunner.class);

	private JobParametersConverter converter = new DefaultJobParametersConverter();

	private JobLauncher jobLauncher;

	private JobRegistry jobRegistry;

	private JobExplorer jobExplorer;

	private JobRepository jobRepository;

	private String jobNames;

	private Collection<Job> jobs = Collections.emptySet();

	private int order = DEFAULT_ORDER;

	private ApplicationEventPublisher publisher;

	/**
	 * Create a new {@link JobLauncherCommandLineRunner}.
	 * @param jobLauncher to launch jobs
	 * @param jobExplorer to check the job repository for previous executions
	 * @deprecated This constructor is deprecated in favor of
	 * {@link JobLauncherCommandLineRunner#JobLauncherCommandLineRunner(JobLauncher, JobExplorer, JobRepository)}.
	 * A job repository is required to check if a job instance exists with the given
	 * parameters when running a job (which is not possible with the job explorer). This
	 * constructor will be removed in a future version.
	 */
	@Deprecated
	public JobLauncherCommandLineRunner(JobLauncher jobLauncher,
			JobExplorer jobExplorer) {
		this.jobLauncher = jobLauncher;
		this.jobExplorer = jobExplorer;
	}

	/**
	 * Create a new {@link JobLauncherCommandLineRunner}.
	 * @param jobLauncher to launch jobs
	 * @param jobExplorer to check the job repository for previous executions
	 * @param jobRepository to check if a job instance exists with the given parameters
	 * when running a job
	 */
	public JobLauncherCommandLineRunner(JobLauncher jobLauncher, JobExplorer jobExplorer,
			JobRepository jobRepository) {
		this.jobLauncher = jobLauncher;
		this.jobExplorer = jobExplorer;
		this.jobRepository = jobRepository;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	@Autowired(required = false)
	public void setJobRegistry(JobRegistry jobRegistry) {
		this.jobRegistry = jobRegistry;
	}

	public void setJobNames(String jobNames) {
		this.jobNames = jobNames;
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
				catch (NoSuchJobException ex) {
					logger.debug("No job found in registry for job name: " + jobName);
				}
			}
		}
	}

	protected void execute(Job job, JobParameters jobParameters)
			throws JobExecutionAlreadyRunningException, JobRestartException,
			JobInstanceAlreadyCompleteException, JobParametersInvalidException,
			JobParametersNotFoundException {
		String jobName = job.getName();
		JobParameters parameters = jobParameters;
		boolean jobInstanceExists = this.jobRepository.isJobInstanceExists(jobName,
				parameters);
		if (jobInstanceExists) {
			JobExecution lastJobExecution = this.jobRepository
					.getLastJobExecution(jobName, jobParameters);
			if (lastJobExecution != null && isStoppedOrFailed(lastJobExecution)
					&& job.isRestartable()) {
				// Retry a failed or stopped execution with previous parameters
				JobParameters previousParameters = lastJobExecution.getJobParameters();
				/*
				 * remove Non-identifying parameters from the previous execution's
				 * parameters since there is no way to remove them programmatically. If
				 * they are required (or need to be modified) on a restart, they need to
				 * be (re)specified.
				 */
				JobParameters previousIdentifyingParameters = removeNonIdentifying(
						previousParameters);
				// merge additional parameters with previous ones (overriding those with
				// the same key)
				parameters = merge(previousIdentifyingParameters, jobParameters);
			}
		}
		else {
			JobParametersIncrementer incrementer = job.getJobParametersIncrementer();
			if (incrementer != null) {
				JobParameters nextParameters = new JobParametersBuilder(jobParameters,
						this.jobExplorer).getNextJobParameters(job).toJobParameters();
				parameters = merge(nextParameters, jobParameters);
			}
		}
		JobExecution execution = this.jobLauncher.run(job, parameters);
		if (this.publisher != null) {
			this.publisher.publishEvent(new JobExecutionEvent(execution));
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

	private JobParameters removeNonIdentifying(JobParameters parameters) {
		Map<String, JobParameter> parameterMap = parameters.getParameters();
		HashMap<String, JobParameter> copy = new HashMap<>(parameterMap);
		for (Map.Entry<String, JobParameter> parameter : copy.entrySet()) {
			if (!parameter.getValue().isIdentifying()) {
				parameterMap.remove(parameter.getKey());
			}
		}
		return new JobParameters(parameterMap);
	}

	private boolean isStoppedOrFailed(JobExecution execution) {
		BatchStatus status = execution.getStatus();
		return (status == BatchStatus.STOPPED || status == BatchStatus.FAILED);
	}

	private JobParameters merge(JobParameters parameters, JobParameters additionals) {
		Map<String, JobParameter> merged = new HashMap<>();
		merged.putAll(parameters.getParameters());
		merged.putAll(additionals.getParameters());
		return new JobParameters(merged);
	}

}
