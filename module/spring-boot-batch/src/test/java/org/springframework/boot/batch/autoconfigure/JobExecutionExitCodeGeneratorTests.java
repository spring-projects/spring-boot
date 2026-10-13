/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.batch.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JobExecutionExitCodeGenerator}.
 *
 * @author Dave Syer
 */
class JobExecutionExitCodeGeneratorTests {

	private final JobExecutionExitCodeGenerator generator = new JobExecutionExitCodeGenerator();

	@Test
	void testExitCodeForRunning() {
		this.generator.onApplicationEvent(new JobExecutionEvent(testJobExecution()));
		assertThat(this.generator.getExitCode()).isOne();
	}

	@Test
	void testExitCodeForCompleted() {
		JobExecution execution = testJobExecution();
		execution.setStatus(BatchStatus.COMPLETED);
		this.generator.onApplicationEvent(new JobExecutionEvent(execution));
		assertThat(this.generator.getExitCode()).isZero();
	}

	@Test
	void testExitCodeForFailed() {
		JobExecution execution = testJobExecution();
		execution.setStatus(BatchStatus.FAILED);
		this.generator.onApplicationEvent(new JobExecutionEvent(execution));
		assertThat(this.generator.getExitCode()).isEqualTo(5);
	}

	private static JobExecution testJobExecution() {
		JobInstance jobInstance = new JobInstance(1L, "job");
		return new JobExecution(0L, jobInstance, new JobParameters());
	}

}
