/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.batch.autoconfigure;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.context.ApplicationEvent;

/**
 * Spring {@link ApplicationEvent} encapsulating a {@link JobExecution}.
 *
 * @author Dave Syer
 * @since 4.0.0
 */
@SuppressWarnings("serial")
public class JobExecutionEvent extends ApplicationEvent {

	private final JobExecution execution;

	/**
	 * Create a new {@link JobExecutionEvent} instance.
	 * @param execution the job execution
	 */
	public JobExecutionEvent(JobExecution execution) {
		super(execution);
		this.execution = execution;
	}

	/**
	 * Return the job execution.
	 * @return the job execution
	 */
	public JobExecution getJobExecution() {
		return this.execution;
	}

}
