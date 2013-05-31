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

package org.springframework.bootstrap.autoconfigure.batch;

import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.bootstrap.CommandLineRunner;
import org.springframework.bootstrap.ExitCodeGenerator;
import org.springframework.bootstrap.context.annotation.ConditionalOnBean;
import org.springframework.bootstrap.context.annotation.ConditionalOnClass;
import org.springframework.bootstrap.context.annotation.ConditionalOnMissingBean;
import org.springframework.bootstrap.context.annotation.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Batch.
 * 
 * @author Dave Syer
 */
@Configuration
@ConditionalOnClass({ JobLauncher.class })
public class BatchAutoConfiguration {

	@Bean
	// Harmless to always include this, but maybe could make it conditional as well
	public BatchDatabaseInitializer batchDatabaseInitializer() {
		return new BatchDatabaseInitializer();
	}

	@Bean
	@ConditionalOnMissingBean(CommandLineRunner.class)
	@ConditionalOnBean(JobLauncher.class)
	public JobLauncherCommandLineRunner jobLauncherCommandLineRunner() {
		return new JobLauncherCommandLineRunner();
	}

	@Bean
	@ConditionalOnMissingBean(ExitCodeGenerator.class)
	@ConditionalOnBean(JobLauncher.class)
	public ExitCodeGenerator jobExecutionExitCodeGenerator() {
		return new JobExecutionExitCodeGenerator();
	}

}
