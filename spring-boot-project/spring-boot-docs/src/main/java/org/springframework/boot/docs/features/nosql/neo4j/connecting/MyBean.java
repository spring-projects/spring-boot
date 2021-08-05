/*
 * Copyright 2012-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.docs.features.nosql.neo4j.connecting;

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
			return session.writeTransaction((transaction) -> transaction
					.run("CREATE (a:Greeting) SET a.message = $message RETURN a.message + ', from node ' + id(a)",
							Values.parameters("message", message))
					.single().get(0).asString());
		}
	}
	// @fold:off

}
