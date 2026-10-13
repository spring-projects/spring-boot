/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.batch.mongodb.autoconfigure;

import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.autoconfigure.AbstractDependsOnBeanFactoryPostProcessor;

/**
 * {@link AbstractDependsOnBeanFactoryPostProcessor} implementation that makes sure Spring
 * Batch's {@link JobRepository} depends on schema initialization.
 *
 * @author Stephane Nicoll
 */
class JobRepositoryDependsOnSchemaInitializationDetector extends AbstractDependsOnBeanFactoryPostProcessor {

	/**
	 * Bean name responsible for schema initialization.
	 */
	static final String SCHEMA_INITIALIZATION_BEAN_NAME = "batchMongoDataInitializingBean";

	JobRepositoryDependsOnSchemaInitializationDetector() {
		super(JobRepository.class, SCHEMA_INITIALIZATION_BEAN_NAME);
	}

}
