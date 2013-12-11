/*
 * Copyright 2012-2013 the original author or authors.
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

import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Batch. By default all
 * jobs in the context will be executed on startup (disable this behaviour with
 * <code>spring.boot.exec.enabled=false</code>). User can supply a job name to execute on
 * startup with <code>spring.batch.exec.name=...</code>.
 * 
 * @author Dave Syer
 */
@Configuration
@ConditionalOnClass({ JobLauncher.class })
public class BatchAutoConfiguration {

	@Value("${spring.batch.job.name:}")
	private String jobName;

	@Bean
	@ConditionalOnMissingBean(BatchDatabaseInitializer.class)
	public BatchDatabaseInitializer batchDatabaseInitializer() {
		return new BatchDatabaseInitializer();
	}

	@Bean
	@ConditionalOnMissingBean(JobLauncherCommandLineRunner.class)
	@ConditionalOnBean(JobLauncher.class)
	@ConditionalOnExpression("${spring.batch.job.enabled:true}")
	public JobLauncherCommandLineRunner jobLauncherCommandLineRunner() {
		JobLauncherCommandLineRunner runner = new JobLauncherCommandLineRunner();
		if (StringUtils.hasText(this.jobName)) {
			runner.setJobName(this.jobName);
		}
		return runner;
	}

	@Bean
	@ConditionalOnMissingBean(ExitCodeGenerator.class)
	@ConditionalOnBean(JobLauncher.class)
	public ExitCodeGenerator jobExecutionExitCodeGenerator() {
		return new JobExecutionExitCodeGenerator();
	}

}
