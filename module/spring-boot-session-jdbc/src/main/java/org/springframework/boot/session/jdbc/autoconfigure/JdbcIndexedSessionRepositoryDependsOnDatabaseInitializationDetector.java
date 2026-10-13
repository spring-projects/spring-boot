/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.session.jdbc.autoconfigure;

import java.util.Collections;
import java.util.Set;

import org.springframework.boot.sql.init.dependency.AbstractBeansOfTypeDependsOnDatabaseInitializationDetector;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitializationDetector;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;

/**
 *
 * {@link DependsOnDatabaseInitializationDetector} for
 * {@link JdbcIndexedSessionRepository}.
 *
 * @author Andy Wilkinson
 */
class JdbcIndexedSessionRepositoryDependsOnDatabaseInitializationDetector
		extends AbstractBeansOfTypeDependsOnDatabaseInitializationDetector {

	@Override
	protected Set<Class<?>> getDependsOnDatabaseInitializationBeanTypes() {
		return Collections.singleton(JdbcIndexedSessionRepository.class);
	}

}
