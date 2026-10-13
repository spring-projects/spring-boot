/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.init;

import java.util.Collections;
import java.util.Set;

import org.springframework.boot.sql.init.dependency.AbstractBeansOfTypeDatabaseInitializerDetector;
import org.springframework.boot.sql.init.dependency.DatabaseInitializerDetector;
import org.springframework.core.Ordered;

/**
 * A {@link DatabaseInitializerDetector} for {@link DataSourceScriptDatabaseInitializer}.
 *
 * @author Andy Wilkinson
 */
class DataSourceScriptDatabaseInitializerDetector extends AbstractBeansOfTypeDatabaseInitializerDetector {

	static final int PRECEDENCE = Ordered.LOWEST_PRECEDENCE - 100;

	@Override
	protected Set<Class<?>> getDatabaseInitializerBeanTypes() {
		return Collections.singleton(DataSourceScriptDatabaseInitializer.class);
	}

	@Override
	public int getOrder() {
		return PRECEDENCE;
	}

}
