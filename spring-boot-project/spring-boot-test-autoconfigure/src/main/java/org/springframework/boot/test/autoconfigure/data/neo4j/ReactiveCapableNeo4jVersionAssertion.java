/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.data.neo4j;

import org.neo4j.driver.Driver;
import org.neo4j.driver.internal.util.ServerVersion;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;

/**
 * This configuration is imported by {@link ReactiveDataNeo4jTest @ReactiveDataNeo4jTest}
 * so that a sane exception message is delivered when a user doesn't upgrade the test
 * harness to 4.0.
 *
 * @author Michael J. Simons
 */
@Configuration(proxyBeanMethods = false)
class ReactiveCapableNeo4jVersionAssertion implements InitializingBean {

	private final Driver driver;

	private static final String ERROR_MESSAGE = "@ReactiveDataNeo4jTest requires at least Neo4j version 4.0."
			+ " You can use a custom image with Testcontainers or if you are on JDK 11+,"
			+ " you can also use the Neo4j Test-Harness org.neo4j.test:neo4j-harness.";

	ReactiveCapableNeo4jVersionAssertion(Driver driver) {
		this.driver = driver;
	}

	@Override
	public void afterPropertiesSet() {

		ServerVersion version = ServerVersion.version(this.driver);
		if (version.lessThan(ServerVersion.v4_0_0)) {
			throw new Neo4jVersionMismatchException(ERROR_MESSAGE, ServerVersion.v4_0_0.toString(), version.toString());
		}
	}

}
