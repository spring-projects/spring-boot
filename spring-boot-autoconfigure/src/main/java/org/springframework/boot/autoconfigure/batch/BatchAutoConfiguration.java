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

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.springframework.batch.core.configuration.ListableJobLocator;
import org.springframework.batch.core.configuration.annotation.BatchConfigurer;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.support.SimpleJobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
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
@ConditionalOnClass({ JobLauncher.class, DataSource.class, JdbcOperations.class })
@AutoConfigureAfter(HibernateJpaAutoConfiguration.class)
@ConditionalOnBean(JobLauncher.class)
public class BatchAutoConfiguration {

	@Value("${spring.batch.job.name:}")
	private String jobName;

	@Autowired(required = false)
	private JobParametersConverter jobParametersConverter;

	@Bean
	@ConditionalOnMissingBean
	public BatchDatabaseInitializer batchDatabaseInitializer() {
		return new BatchDatabaseInitializer();
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnExpression("${spring.batch.job.enabled:true}")
	public JobLauncherCommandLineRunner jobLauncherCommandLineRunner() {
		JobLauncherCommandLineRunner runner = new JobLauncherCommandLineRunner();
		if (StringUtils.hasText(this.jobName)) {
			runner.setJobName(this.jobName);
		}
		return runner;
	}

	@Bean
	@ConditionalOnMissingBean
	public ExitCodeGenerator jobExecutionExitCodeGenerator() {
		return new JobExecutionExitCodeGenerator();
	}

	@Bean
	@ConditionalOnMissingBean
	public JobExplorer jobExplorer(DataSource dataSource) throws Exception {
		JobExplorerFactoryBean factory = new JobExplorerFactoryBean();
		factory.setDataSource(dataSource);
		factory.afterPropertiesSet();
		return (JobExplorer) factory.getObject();
	}

	@Bean
	@ConditionalOnMissingBean
	public JobOperator jobOperator(JobExplorer jobExplorer, JobLauncher jobLauncher,
			ListableJobLocator jobRegistry, JobRepository jobRepository) throws Exception {
		SimpleJobOperator factory = new SimpleJobOperator();
		factory.setJobExplorer(jobExplorer);
		factory.setJobLauncher(jobLauncher);
		factory.setJobRegistry(jobRegistry);
		factory.setJobRepository(jobRepository);
		if (this.jobParametersConverter != null) {
			factory.setJobParametersConverter(this.jobParametersConverter);
		}
		return factory;
	}

	@ConditionalOnClass(name = "javax.persistence.EntityManagerFactory")
	@ConditionalOnMissingBean(BatchConfigurer.class)
	@Configuration
	protected static class JpaBatchConfiguration {

		// The EntityManagerFactory may not be discoverable by type when this condition
		// is evaluated, so we need a well-known bean name. This is the one used by Spring
		// Boot in the JPA auto configuration.
		@Bean
		@ConditionalOnBean(name = "entityManagerFactory")
		public BatchConfigurer jpaBatchConfigurer(DataSource dataSource,
				EntityManagerFactory entityManagerFactory) {
			return new BasicBatchConfigurer(dataSource, entityManagerFactory);
		}

		@Bean
		@ConditionalOnMissingBean(name = "entityManagerFactory")
		public BatchConfigurer basicBatchConfigurer(DataSource dataSource) {
			return new BasicBatchConfigurer(dataSource);
		}

	}

}
