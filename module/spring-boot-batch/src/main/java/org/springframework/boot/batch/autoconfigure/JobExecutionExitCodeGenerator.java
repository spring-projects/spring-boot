/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.batch.autoconfigure;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.context.ApplicationListener;

/**
 * {@link ExitCodeGenerator} for {@link JobExecutionEvent}s.
 *
 * @author Dave Syer
 * @since 4.0.0
 */
public class JobExecutionExitCodeGenerator implements ApplicationListener<JobExecutionEvent>, ExitCodeGenerator {

	private final List<JobExecution> executions = new CopyOnWriteArrayList<>();

	@Override
	public void onApplicationEvent(JobExecutionEvent event) {
		this.executions.add(event.getJobExecution());
	}

	@Override
	public int getExitCode() {
		for (JobExecution execution : this.executions) {
			if (execution.getStatus().ordinal() > 0) {
				return execution.getStatus().ordinal();
			}
		}
		return 0;
	}

}
