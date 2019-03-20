/*
 * Copyright 2012-2017 the original author or authors.
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

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.configuration.annotation.BatchConfigurer;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StringUtils;

/**
 * Basic {@link BatchConfigurer} implementation.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Kazuki Shimizu
 */
public class BasicBatchConfigurer implements BatchConfigurer {

	private static final Log logger = LogFactory.getLog(BasicBatchConfigurer.class);

	private final BatchProperties properties;

	private final DataSource dataSource;

	private final EntityManagerFactory entityManagerFactory;

	private PlatformTransactionManager transactionManager;

	private final TransactionManagerCustomizers transactionManagerCustomizers;

	private JobRepository jobRepository;

	private JobLauncher jobLauncher;

	private JobExplorer jobExplorer;

	/**
	 * Create a new {@link BasicBatchConfigurer} instance.
	 * @param properties the batch properties
	 * @param dataSource the underlying data source
	 * @param transactionManagerCustomizers transaction manager customizers (or
	 * {@code null})
	 */
	protected BasicBatchConfigurer(BatchProperties properties, DataSource dataSource,
			TransactionManagerCustomizers transactionManagerCustomizers) {
		this(properties, dataSource, null, transactionManagerCustomizers);
	}

	/**
	 * Create a new {@link BasicBatchConfigurer} instance.
	 * @param properties the batch properties
	 * @param dataSource the underlying data source
	 * @param entityManagerFactory the entity manager factory (or {@code null})
	 * @param transactionManagerCustomizers transaction manager customizers (or
	 * {@code null})
	 */
	protected BasicBatchConfigurer(BatchProperties properties, DataSource dataSource,
			EntityManagerFactory entityManagerFactory,
			TransactionManagerCustomizers transactionManagerCustomizers) {
		this.properties = properties;
		this.entityManagerFactory = entityManagerFactory;
		this.dataSource = dataSource;
		this.transactionManagerCustomizers = transactionManagerCustomizers;
	}

	@Override
	public JobRepository getJobRepository() {
		return this.jobRepository;
	}

	@Override
	public PlatformTransactionManager getTransactionManager() {
		return this.transactionManager;
	}

	@Override
	public JobLauncher getJobLauncher() {
		return this.jobLauncher;
	}

	@Override
	public JobExplorer getJobExplorer() throws Exception {
		return this.jobExplorer;
	}

	@PostConstruct
	public void initialize() {
		try {
			this.transactionManager = createTransactionManager();
			this.jobRepository = createJobRepository();
			this.jobLauncher = createJobLauncher();
			this.jobExplorer = createJobExplorer();
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to initialize Spring Batch", ex);
		}
	}

	protected JobExplorer createJobExplorer() throws Exception {
		JobExplorerFactoryBean jobExplorerFactoryBean = new JobExplorerFactoryBean();
		jobExplorerFactoryBean.setDataSource(this.dataSource);
		String tablePrefix = this.properties.getTablePrefix();
		if (StringUtils.hasText(tablePrefix)) {
			jobExplorerFactoryBean.setTablePrefix(tablePrefix);
		}
		jobExplorerFactoryBean.afterPropertiesSet();
		return jobExplorerFactoryBean.getObject();
	}

	protected JobLauncher createJobLauncher() throws Exception {
		SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
		jobLauncher.setJobRepository(getJobRepository());
		jobLauncher.afterPropertiesSet();
		return jobLauncher;
	}

	protected JobRepository createJobRepository() throws Exception {
		JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
		factory.setDataSource(this.dataSource);
		if (this.entityManagerFactory != null) {
			logger.warn(
					"JPA does not support custom isolation levels, so locks may not be taken when launching Jobs");
			factory.setIsolationLevelForCreate("ISOLATION_DEFAULT");
		}
		String tablePrefix = this.properties.getTablePrefix();
		if (StringUtils.hasText(tablePrefix)) {
			factory.setTablePrefix(tablePrefix);
		}
		factory.setTransactionManager(getTransactionManager());
		factory.afterPropertiesSet();
		return factory.getObject();
	}

	protected PlatformTransactionManager createTransactionManager() {
		PlatformTransactionManager transactionManager = createAppropriateTransactionManager();
		if (this.transactionManagerCustomizers != null) {
			this.transactionManagerCustomizers.customize(transactionManager);
		}
		return transactionManager;
	}

	private PlatformTransactionManager createAppropriateTransactionManager() {
		if (this.entityManagerFactory != null) {
			return new JpaTransactionManager(this.entityManagerFactory);
		}
		return new DataSourceTransactionManager(this.dataSource);
	}

}
