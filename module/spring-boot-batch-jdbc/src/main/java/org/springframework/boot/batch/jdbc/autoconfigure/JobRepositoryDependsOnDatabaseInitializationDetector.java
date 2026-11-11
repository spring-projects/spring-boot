/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.batch.jdbc.autoconfigure;

import java.util.Collections;
import java.util.Set;

import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.sql.init.dependency.AbstractBeansOfTypeDependsOnDatabaseInitializationDetector;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitializationDetector;

/**
 * {@link DependsOnDatabaseInitializationDetector} for Spring Batch's
 * {@link JobRepository}.
 *
 * @author Henning Pöttker
 */
class JobRepositoryDependsOnDatabaseInitializationDetector
		extends AbstractBeansOfTypeDependsOnDatabaseInitializationDetector {

	@Override
	protected Set<Class<?>> getDependsOnDatabaseInitializationBeanTypes() {
		return Collections.singleton(JobRepository.class);
	}

}
