/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.data.nosql.neo4j.repositories

import org.neo4j.driver.Driver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager

@Configuration(proxyBeanMethods = false)
class MyNeo4jConfiguration {

	@Bean
	fun reactiveTransactionManager(driver: Driver,
			databaseNameProvider: ReactiveDatabaseSelectionProvider): ReactiveNeo4jTransactionManager {
		return ReactiveNeo4jTransactionManager(driver, databaseNameProvider)
	}
}

