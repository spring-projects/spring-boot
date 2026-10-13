/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.flyway.autoconfigure;

import java.util.Collections;
import java.util.Set;

import org.springframework.boot.sql.init.dependency.AbstractBeansOfTypeDatabaseInitializerDetector;
import org.springframework.boot.sql.init.dependency.DatabaseInitializerDetector;

/**
 * A {@link DatabaseInitializerDetector} for {@link FlywayMigrationInitializer}.
 *
 * @author Andy Wilkinson
 */
class FlywayMigrationInitializerDatabaseInitializerDetector extends AbstractBeansOfTypeDatabaseInitializerDetector {

	@Override
	protected Set<Class<?>> getDatabaseInitializerBeanTypes() {
		return Collections.singleton(FlywayMigrationInitializer.class);
	}

	@Override
	public int getOrder() {
		return 1;
	}

}
