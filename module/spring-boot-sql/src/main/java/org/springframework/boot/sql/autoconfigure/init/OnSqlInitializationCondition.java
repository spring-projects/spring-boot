/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.sql.autoconfigure.init;

import org.springframework.context.annotation.Condition;

/**
 * {@link Condition} implementation for {@link ConditionalOnSqlInitialization}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class OnSqlInitializationCondition extends OnDatabaseInitializationCondition {

	OnSqlInitializationCondition() {
		super("SQL Initialization", "spring.sql.init.mode");
	}

}
