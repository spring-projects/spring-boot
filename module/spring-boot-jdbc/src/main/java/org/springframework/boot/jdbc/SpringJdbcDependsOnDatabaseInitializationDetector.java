/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc;

import java.util.Set;

import org.springframework.boot.sql.init.dependency.AbstractBeansOfTypeDependsOnDatabaseInitializationDetector;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitializationDetector;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * {@link DependsOnDatabaseInitializationDetector} for Spring Framework's JDBC support.
 *
 * @author Andy Wilkinson
 */
class SpringJdbcDependsOnDatabaseInitializationDetector
		extends AbstractBeansOfTypeDependsOnDatabaseInitializationDetector {

	@Override
	protected Set<Class<?>> getDependsOnDatabaseInitializationBeanTypes() {
		return Set.of(JdbcClient.class, JdbcOperations.class, NamedParameterJdbcOperations.class);
	}

}
