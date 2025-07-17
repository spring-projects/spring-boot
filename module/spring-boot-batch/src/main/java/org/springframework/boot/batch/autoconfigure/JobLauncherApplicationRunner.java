/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.batch.autoconfigure;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobExecutionException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
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
 * {@link ApplicationRunner} to {@link JobOperator launch} Spring Batch jobs. If a single
 * job is found in the context, it will be executed by default. If multiple jobs are
 * found, launch a specific job by providing a jobName.
 *
 * @author Dave Syer
 * @author Jean-Pierre Bergamin
 * @author Mahmoud Ben Hassine
 * @author Stephane Nicoll
 * @author Akshay Dubey
 * @since 4.0.0
 */
public class JobLauncherApplicationRunner
		implements ApplicationRunner, InitializingBean, Ordered, ApplicationEventPublisherAware {

	/**
	 * The default order for the command line runner.
	 */
	public static final int DEFAULT_ORDER = 0;

	private static final Log logger = LogFactory.getLog(JobLauncherApplicationRunner.class);

	private JobParametersConverter converter = new DefaultJobParametersConverter();

	private final JobOperator jobOperator;

	private JobRegistry jobRegistry;

	private String jobName;

	private Collection<Job> jobs = Collections.emptySet();

	private int order = DEFAULT_ORDER;

	private ApplicationEventPublisher publisher;

	/**
	 * Create a new {@link JobLauncherApplicationRunner}.
	 * @param jobOperator to launch jobs
	 */
	public JobLauncherApplicationRunner(JobOperator jobOperator) {
		Assert.notNull(jobOperator, "'jobOperator' must not be null");
		this.jobOperator = jobOperator;
	}

	@Override
	public void afterPropertiesSet() {
		Assert.state(this.jobs.size() <= 1 || StringUtils.hasText(this.jobName),
				"Job name must be specified in case of multiple jobs");
		if (StringUtils.hasText(this.jobName)) {
			Assert.state(isLocalJob(this.jobName) || isRegisteredJob(this.jobName),
					() -> "No job found with name '" + this.jobName + "'");
		}
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

	public void setJobName(String jobName) {
		this.jobName = jobName;
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
	public void run(ApplicationArguments args) throws Exception {
		String[] jobArguments = args.getNonOptionArgs().toArray(new String[0]);
		run(jobArguments);
	}

	public void run(String... args) throws JobExecutionException {
		logger.info("Running default command line with: " + Arrays.asList(args));
		launchJobFromProperties(StringUtils.splitArrayElementsIntoProperties(args, "="));
	}

	protected void launchJobFromProperties(Properties properties) throws JobExecutionException {
		JobParameters jobParameters = this.converter.getJobParameters(properties);
		executeLocalJobs(jobParameters);
		executeRegisteredJobs(jobParameters);
	}

	private boolean isLocalJob(String jobName) {
		return this.jobs.stream().anyMatch((job) -> job.getName().equals(jobName));
	}

	private boolean isRegisteredJob(String jobName) {
		return this.jobRegistry != null && this.jobRegistry.getJobNames().contains(jobName);
	}

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

	private void executeRegisteredJobs(JobParameters jobParameters) throws JobExecutionException {
		if (this.jobRegistry != null && StringUtils.hasText(this.jobName)) {
			if (!isLocalJob(this.jobName)) {
				Job job = this.jobRegistry.getJob(this.jobName);
				execute(job, jobParameters);
			}
		}
	}

	protected void execute(Job job, JobParameters jobParameters)
			throws JobExecutionAlreadyRunningException, NoSuchJobException, JobRestartException,
			JobInstanceAlreadyCompleteException, JobParametersInvalidException {
		JobExecution execution = this.jobOperator.start(job, jobParameters);
		if (this.publisher != null) {
			this.publisher.publishEvent(new JobExecutionEvent(execution));
		}
	}

}
