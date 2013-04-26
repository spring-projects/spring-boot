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

import java.util.Arrays;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.bootstrap.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
// FIXME: what to do with more than one Job?
public class JobLauncherCommandLineRunner implements CommandLineRunner {

	private static Log logger = LogFactory.getLog(JobLauncherCommandLineRunner.class);

	@Autowired(required = false)
	private JobParametersConverter converter = new DefaultJobParametersConverter();

	@Autowired
	private JobLauncher jobLauncher;

	@Autowired
	private Job job;

	public void run(String... args) {
		logger.info("Running default command line with: " + Arrays.asList(args));
		launchJobFromProperties(StringUtils.splitArrayElementsIntoProperties(args,
				"="));
	}

	protected void launchJobFromProperties(Properties properties) {
		try {
			this.jobLauncher.run(this.job,
					this.converter.getJobParameters(properties));
		} catch (JobExecutionException e) {
			throw new IllegalStateException("Could not run job", e);
		}
	}
}