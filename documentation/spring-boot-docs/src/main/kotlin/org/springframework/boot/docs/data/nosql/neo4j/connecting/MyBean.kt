/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.data.nosql.neo4j.connecting

import org.neo4j.driver.Driver
import org.neo4j.driver.TransactionContext
import org.neo4j.driver.Values
import org.springframework.stereotype.Component

@Component
class MyBean(private val driver: Driver) {
	// @fold:on // ...
	fun someMethod(message: String?): String {
		driver.session().use { session ->
			return@someMethod session.executeWrite { transaction: TransactionContext ->
				transaction
					.run(
						"CREATE (a:Greeting) SET a.message = \$message RETURN a.message + ', from node ' + id(a)",
						Values.parameters("message", message)
					)
					.single()[0].asString()
			}
		}
	}
	// @fold:off
}

