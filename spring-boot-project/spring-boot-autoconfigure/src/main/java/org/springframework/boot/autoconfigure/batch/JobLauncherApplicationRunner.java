/*
 * Copyright 2012-2023 the original author or authors.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
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
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.Ordered;
import org.springframework.core.log.LogMessage;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link ApplicationRunner} to {@link JobLauncher launch} Spring Batch jobs. If a single
 * job is found in the context, it will be executed by default. If multiple jobs are
 * found, launch a specific job by providing a jobName.
 *
 * @author Dave Syer
 * @author Jean-Pierre Bergamin
 * @author Mahmoud Ben Hassine
 * @author Stephane Nicoll
 * @author Akshay Dubey
 * @since 2.3.0
 */
public class JobLauncherApplicationRunner
		implements ApplicationRunner, InitializingBean, Ordered, ApplicationEventPublisherAware {

	/**
	 * The default order for the command line runner.
	 */
	public static final int DEFAULT_ORDER = 0;

	private static final Log logger = LogFactory.getLog(JobLauncherApplicationRunner.class);

	private JobParametersConverter converter = new DefaultJobParametersConverter();

	private final JobLauncher jobLauncher;

	private final JobExplorer jobExplorer;

	private final JobRepository jobRepository;

	private JobRegistry jobRegistry;

	private String jobName;

	private Collection<Job> jobs = Collections.emptySet();

	private int order = DEFAULT_ORDER;

	private ApplicationEventPublisher publisher;

	/**
	 * Create a new {@link JobLauncherApplicationRunner}.
	 * @param jobLauncher to launch jobs
	 * @param jobExplorer to check the job repository for previous executions
	 * @param jobRepository to check if a job instance exists with the given parameters
	 * when running a job
	 */
	public JobLauncherApplicationRunner(JobLauncher jobLauncher, JobExplorer jobExplorer, JobRepository jobRepository) {
		Assert.notNull(jobLauncher, "JobLauncher must not be null");
		Assert.notNull(jobExplorer, "JobExplorer must not be null");
		Assert.notNull(jobRepository, "JobRepository must not be null");
		this.jobLauncher = jobLauncher;
		this.jobExplorer = jobExplorer;
		this.jobRepository = jobRepository;
	}

	/**
	 * This method is called after all the properties have been set for the
	 * JobLauncherApplicationRunner class. It checks if there are multiple jobs and if a
	 * job name is specified. If a job name is specified, it checks if the job is either a
	 * local job or a registered job. If the job name is not specified, it throws an
	 * exception. If the job name is specified but no job is found with that name, it
	 * throws an exception.
	 */
	@Override
	public void afterPropertiesSet() {
		Assert.isTrue(this.jobs.size() <= 1 || StringUtils.hasText(this.jobName),
				"Job name must be specified in case of multiple jobs");
		if (StringUtils.hasText(this.jobName)) {
			Assert.isTrue(isLocalJob(this.jobName) || isRegisteredJob(this.jobName),
					() -> "No job found with name '" + this.jobName + "'");
		}
	}

	/**
	 * Validates the JobLauncherApplicationRunner.
	 * @deprecated This method has been deprecated since version 3.0.10 and will be
	 * removed in a future release. Use the {@link #afterPropertiesSet()} method instead.
	 */
	@Deprecated(since = "3.0.10", forRemoval = true)
	public void validate() {
		afterPropertiesSet();
	}

	/**
	 * Sets the order in which the JobLauncherApplicationRunner should be executed.
	 * @param order the order value to set
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	/**
	 * Returns the order of this JobLauncherApplicationRunner.
	 * @return the order of this JobLauncherApplicationRunner
	 */
	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * Sets the application event publisher.
	 * @param publisher the application event publisher to set
	 */
	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	/**
	 * Sets the JobRegistry for this JobLauncherApplicationRunner.
	 * @param jobRegistry the JobRegistry to be set
	 */
	@Autowired(required = false)
	public void setJobRegistry(JobRegistry jobRegistry) {
		this.jobRegistry = jobRegistry;
	}

	/**
	 * Sets the job name for the JobLauncherApplicationRunner.
	 * @param jobName the name of the job to be set
	 */
	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	/**
	 * Sets the JobParametersConverter for this JobLauncherApplicationRunner.
	 * @param converter the JobParametersConverter to be set
	 */
	@Autowired(required = false)
	public void setJobParametersConverter(JobParametersConverter converter) {
		this.converter = converter;
	}

	/**
	 * Sets the collection of jobs.
	 * @param jobs the collection of jobs to be set
	 */
	@Autowired(required = false)
	public void setJobs(Collection<Job> jobs) {
		this.jobs = jobs;
	}

	/**
	 * Runs the application with the given arguments.
	 * @param args the command line arguments passed to the application
	 * @throws Exception if an error occurs while running the application
	 */
	@Override
	public void run(ApplicationArguments args) throws Exception {
		String[] jobArguments = args.getNonOptionArgs().toArray(new String[0]);
		run(jobArguments);
	}

	/**
	 * Runs the default command line with the given arguments.
	 * @param args the command line arguments
	 * @throws JobExecutionException if there is an error executing the job
	 */
	public void run(String... args) throws JobExecutionException {
		logger.info("Running default command line with: " + Arrays.asList(args));
		launchJobFromProperties(StringUtils.splitArrayElementsIntoProperties(args, "="));
	}

	/**
	 * Launches a job using the provided properties.
	 * @param properties the properties containing the job parameters
	 * @throws JobExecutionException if there is an error executing the job
	 */
	protected void launchJobFromProperties(Properties properties) throws JobExecutionException {
		JobParameters jobParameters = this.converter.getJobParameters(properties);
		executeLocalJobs(jobParameters);
		executeRegisteredJobs(jobParameters);
	}

	/**
	 * Checks if a job with the given name is a local job.
	 * @param jobName the name of the job to check
	 * @return true if the job is a local job, false otherwise
	 */
	private boolean isLocalJob(String jobName) {
		return this.jobs.stream().anyMatch((job) -> job.getName().equals(jobName));
	}

	/**
	 * Checks if a job with the given name is registered in the job registry.
	 * @param jobName the name of the job to check
	 * @return true if the job is registered, false otherwise
	 */
	private boolean isRegisteredJob(String jobName) {
		return this.jobRegistry != null && this.jobRegistry.getJobNames().contains(jobName);
	}

	/**
	 * Executes local jobs with the given job parameters.
	 * @param jobParameters the job parameters to be used for executing the jobs
	 * @throws JobExecutionException if there is an error executing the jobs
	 */
	private void executeLocalJobs(JobParameters jobParameters) throws JobExecutionException {
		for (Job job : this.jobs) {
			if (StringUtils.hasText(this.jobName)) {
				if (!this.jobName.equals(job.getName())) {
					logger.debug(LogMessage.format("Skipped job: %s", job.getName()));
					continue;
				}
			}
			execute(job, jobParameters);
		}
	}

	/**
	 * Executes the registered jobs with the given job parameters.
	 * @param jobParameters the job parameters to be used for job execution
	 * @throws JobExecutionException if there is an error during job execution
	 */
	private void executeRegisteredJobs(JobParameters jobParameters) throws JobExecutionException {
		if (this.jobRegistry != null && StringUtils.hasText(this.jobName)) {
			if (!isLocalJob(this.jobName)) {
				Job job = this.jobRegistry.getJob(this.jobName);
				execute(job, jobParameters);
			}
		}
	}

	/**
	 * Executes a job with the given job parameters.
	 * @param job the job to be executed
	 * @param jobParameters the job parameters for the execution
	 * @throws JobExecutionAlreadyRunningException if the job is already running
	 * @throws JobRestartException if the job cannot be restarted
	 * @throws JobInstanceAlreadyCompleteException if the job instance is already complete
	 * @throws JobParametersInvalidException if the job parameters are invalid
	 */
	protected void execute(Job job, JobParameters jobParameters) throws JobExecutionAlreadyRunningException,
			JobRestartException, JobInstanceAlreadyCompleteException, JobParametersInvalidException {
		JobParameters parameters = getNextJobParameters(job, jobParameters);
		JobExecution execution = this.jobLauncher.run(job, parameters);
		if (this.publisher != null) {
			this.publisher.publishEvent(new JobExecutionEvent(execution));
		}
	}

	/**
	 * Retrieves the next set of job parameters for the given job and job parameters. If
	 * the job instance already exists in the job repository, it calls the
	 * getNextJobParametersForExisting method. If the job has a job parameters
	 * incrementer, it generates the next set of job parameters using the
	 * JobParametersBuilder and the job explorer. Finally, it merges the generated job
	 * parameters with the existing job parameters and returns the result.
	 * @param job the job for which to retrieve the next job parameters
	 * @param jobParameters the current job parameters
	 * @return the next set of job parameters
	 */
	private JobParameters getNextJobParameters(Job job, JobParameters jobParameters) {
		if (this.jobRepository != null && this.jobRepository.isJobInstanceExists(job.getName(), jobParameters)) {
			return getNextJobParametersForExisting(job, jobParameters);
		}
		if (job.getJobParametersIncrementer() == null) {
			return jobParameters;
		}
		JobParameters nextParameters = new JobParametersBuilder(jobParameters, this.jobExplorer)
			.getNextJobParameters(job)
			.toJobParameters();
		return merge(nextParameters, jobParameters);
	}

	/**
	 * Retrieves the next set of job parameters for an existing job.
	 * @param job The job for which to retrieve the next set of job parameters.
	 * @param jobParameters The current job parameters.
	 * @return The next set of job parameters for the existing job.
	 */
	private JobParameters getNextJobParametersForExisting(Job job, JobParameters jobParameters) {
		JobExecution lastExecution = this.jobRepository.getLastJobExecution(job.getName(), jobParameters);
		if (isStoppedOrFailed(lastExecution) && job.isRestartable()) {
			JobParameters previousIdentifyingParameters = new JobParameters(
					lastExecution.getJobParameters().getIdentifyingParameters());
			return merge(previousIdentifyingParameters, jobParameters);
		}
		return jobParameters;
	}

	/**
	 * Checks if the given JobExecution is stopped or failed.
	 * @param execution the JobExecution to check
	 * @return true if the JobExecution is stopped or failed, false otherwise
	 */
	private boolean isStoppedOrFailed(JobExecution execution) {
		BatchStatus status = (execution != null) ? execution.getStatus() : null;
		return (status == BatchStatus.STOPPED || status == BatchStatus.FAILED);
	}

	/**
	 * Merges the given JobParameters with additional JobParameters.
	 * @param parameters the original JobParameters
	 * @param additionals the additional JobParameters to be merged
	 * @return a new JobParameters object containing the merged parameters
	 */
	private JobParameters merge(JobParameters parameters, JobParameters additionals) {
		Map<String, JobParameter<?>> merged = new LinkedHashMap<>();
		merged.putAll(parameters.getParameters());
		merged.putAll(additionals.getParameters());
		return new JobParameters(merged);
	}

}
