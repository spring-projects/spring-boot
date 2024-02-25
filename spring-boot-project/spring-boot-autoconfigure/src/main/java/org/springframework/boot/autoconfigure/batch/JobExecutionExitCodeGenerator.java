/*
 * Copyright 2012-2022 the original author or authors.
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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.batch.core.JobExecution;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.context.ApplicationListener;

/**
 * {@link ExitCodeGenerator} for {@link JobExecutionEvent}s.
 *
 * @author Dave Syer
 * @since 1.0.0
 */
public class JobExecutionExitCodeGenerator implements ApplicationListener<JobExecutionEvent>, ExitCodeGenerator {

	private final List<JobExecution> executions = new CopyOnWriteArrayList<>();

	/**
     * This method is called when a JobExecutionEvent is triggered.
     * It adds the JobExecution object from the event to the list of executions.
     *
     * @param event the JobExecutionEvent that is triggered
     */
    @Override
	public void onApplicationEvent(JobExecutionEvent event) {
		this.executions.add(event.getJobExecution());
	}

	/**
     * Returns the exit code of the job execution.
     * 
     * @return the exit code of the job execution. If any of the executions have a status ordinal greater than 0, 
     *         the highest ordinal value is returned. If all executions have a status ordinal of 0, 0 is returned.
     */
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
