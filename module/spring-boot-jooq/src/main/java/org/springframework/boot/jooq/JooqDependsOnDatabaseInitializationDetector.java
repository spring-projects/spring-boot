/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jooq;

import java.util.Collections;
import java.util.Set;

import org.jooq.DSLContext;

import org.springframework.boot.sql.init.dependency.AbstractBeansOfTypeDependsOnDatabaseInitializationDetector;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitializationDetector;

/**
 * {@link DependsOnDatabaseInitializationDetector} for jOOQ.
 *
 * @author Andy Wilkinson
 */
class JooqDependsOnDatabaseInitializationDetector extends AbstractBeansOfTypeDependsOnDatabaseInitializationDetector {

	@Override
	protected Set<Class<?>> getDependsOnDatabaseInitializationBeanTypes() {
		return Collections.singleton(DSLContext.class);
	}

}
