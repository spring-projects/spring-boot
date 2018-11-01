/*
 * Copyright 2012-2017 the original author or authors.
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

import org.junit.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JobExecutionExitCodeGenerator}.
 *
 * @author Dave Syer
 * @author Dimitrios Liapis
 */
public class JobExecutionExitCodeGeneratorTests {

	private final JobExecutionExitCodeGenerator generator = new JobExecutionExitCodeGenerator();

	@Test
	public void testExitCodeForRunningNoCustomExitCodeEnabled() {
		this.generator.onApplicationEvent(new JobExecutionEvent(new JobExecution(0L)));
		assertThat(this.generator.getExitCode()).isEqualTo(1);
	}

	@Test
	public void testExitCodeForCompletedNoCustomExitCodeEnabled() {
		JobExecution execution = new JobExecution(0L);
		execution.setStatus(BatchStatus.COMPLETED);
		this.generator.onApplicationEvent(new JobExecutionEvent(execution));
		assertThat(this.generator.getExitCode()).isEqualTo(0);
	}

	@Test
	public void testExitCodeForFailedNoCustomExitCodeEnabled() {
		JobExecution execution = new JobExecution(0L);
		execution.setStatus(BatchStatus.FAILED);
		this.generator.onApplicationEvent(new JobExecutionEvent(execution));
		assertThat(this.generator.getExitCode()).isEqualTo(5);
	}

	@Test
	public void testExitCodeForFailedWithCustomExitCodeEnabled() {
		this.generator.setExitCodeEnabled(true);
		this.generator.setExitCode(-11);
		JobExecution execution = new JobExecution(0L);
		execution.setExitStatus(ExitStatus.FAILED);
		this.generator.onApplicationEvent(new JobExecutionEvent(execution));
		assertThat(this.generator.getExitCode()).isEqualTo(-11);
	}

	@Test
	public void testExitCodeForCompletedWithCustomExitCodeEnabled() {
		this.generator.setExitCodeEnabled(true);
		this.generator.setExitCode(-11);
		JobExecution execution = new JobExecution(0L);
		execution.setStatus(BatchStatus.COMPLETED);
		this.generator.onApplicationEvent(new JobExecutionEvent(execution));
		assertThat(this.generator.getExitCode()).isEqualTo(0);
	}

}
