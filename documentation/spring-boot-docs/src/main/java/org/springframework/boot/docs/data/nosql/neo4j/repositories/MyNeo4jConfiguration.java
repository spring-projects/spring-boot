/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.data.nosql.neo4j.repositories;

import org.neo4j.driver.Driver;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager;

@Configuration(proxyBeanMethods = false)
public class MyNeo4jConfiguration {

	@Bean
	public ReactiveNeo4jTransactionManager reactiveTransactionManager(Driver driver,
			ReactiveDatabaseSelectionProvider databaseNameProvider) {
		return new ReactiveNeo4jTransactionManager(driver, databaseNameProvider);
	}

}
