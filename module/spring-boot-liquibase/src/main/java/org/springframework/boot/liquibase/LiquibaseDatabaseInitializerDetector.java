/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.liquibase;

import java.util.Collections;
import java.util.Set;

import liquibase.integration.spring.SpringLiquibase;

import org.springframework.boot.sql.init.dependency.AbstractBeansOfTypeDatabaseInitializerDetector;
import org.springframework.boot.sql.init.dependency.DatabaseInitializerDetector;

/**
 * A {@link DatabaseInitializerDetector} for Liquibase.
 *
 * @author Andy Wilkinson
 */
class LiquibaseDatabaseInitializerDetector extends AbstractBeansOfTypeDatabaseInitializerDetector {

	@Override
	protected Set<Class<?>> getDatabaseInitializerBeanTypes() {
		return Collections.singleton(SpringLiquibase.class);
	}

}
