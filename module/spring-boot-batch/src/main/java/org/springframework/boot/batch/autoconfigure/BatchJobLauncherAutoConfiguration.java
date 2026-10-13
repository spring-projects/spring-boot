/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.batch.autoconfigure;

import org.springframework.batch.core.launch.JobOperator;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Batch. If a single job is
 * found in the context, it will be executed on startup.
 * <p>
 * Disable this behavior with {@literal spring.batch.job.enabled=false}).
 * <p>
 * If multiple jobs are found, a job name to execute on startup can be supplied by the
 * User with : {@literal spring.batch.job.name=job1}. In this case the Runner will first
 * find jobs registered as Beans, then those in the existing JobRegistry.
 *
 * @author Dave Syer
 * @author Eddú Meléndez
 * @author Kazuki Shimizu
 * @author Mahmoud Ben Hassine
 * @author Lars Uffmann
 * @author Lasse Wulff
 * @author Yanming Zhou
 * @since 4.0.0
 */
@AutoConfiguration(after = BatchAutoConfiguration.class)
@ConditionalOnClass(JobOperator.class)
@ConditionalOnBean(JobOperator.class)
@EnableConfigurationProperties(BatchProperties.class)
public final class BatchJobLauncherAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBooleanProperty(name = "spring.batch.job.enabled", matchIfMissing = true)
	JobLauncherApplicationRunner jobLauncherApplicationRunner(JobOperator jobOperator, BatchProperties properties) {
		JobLauncherApplicationRunner runner = new JobLauncherApplicationRunner(jobOperator);
		String jobName = properties.getJob().getName();
		if (StringUtils.hasText(jobName)) {
			runner.setJobName(jobName);
		}
		return runner;
	}

	@Bean
	@ConditionalOnMissingBean(ExitCodeGenerator.class)
	JobExecutionExitCodeGenerator jobExecutionExitCodeGenerator() {
		return new JobExecutionExitCodeGenerator();
	}

}
