/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.data.nosql.neo4j.connecting;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;

import org.springframework.stereotype.Component;

@Component
public class MyBean {

	private final Driver driver;

	public MyBean(Driver driver) {
		this.driver = driver;
	}

	// @fold:on // ...
	public String someMethod(String message) {
		try (Session session = this.driver.session()) {
			return session.executeWrite(
					(transaction) -> transaction
						.run("CREATE (a:Greeting) SET a.message = $message RETURN a.message + ', from node ' + id(a)",
								Values.parameters("message", message))
						.single()
						.get(0)
						.asString());
		}
	}
	// @fold:off

}
