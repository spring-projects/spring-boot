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

package org.springframework.boot.actuate.neo4j;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import org.neo4j.driver.ConnectionPoolMetrics;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Metrics;
import org.neo4j.driver.exceptions.ClientException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Some predefined mocks, only to be used internally for tests.
 *
 * @author Michael J. Simons
 */
final class Neo4jDriverMocks {

	public static Driver mockDriverWithMetrics() {
		ConnectionPoolMetrics p1 = mock(ConnectionPoolMetrics.class);
		when(p1.id()).thenReturn("p1");

		Metrics metrics = mock(Metrics.class);
		when(metrics.connectionPoolMetrics()).thenReturn(Collections.singletonList(p1));

		Driver driver = mock(Driver.class);
		when(driver.metrics()).thenReturn(metrics);

		when(driver.verifyConnectivityAsync()).thenReturn(CompletableFuture.completedFuture(null));

		return driver;
	}

	public static Driver mockDriverWithoutMetrics() {

		Driver driver = mock(Driver.class);
		when(driver.metrics()).thenThrow(ClientException.class);
		return driver;
	}

	private Neo4jDriverMocks() {
	}

}
