/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.testsupport.testcontainers;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.session.SessionFactory;
import org.rnorth.ducttape.TimeoutException;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.HostPortWaitStrategy;

/**
 * Provides utility methods that allow creation of docker containers for
 * tests.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 */
public abstract class TestContainers {

	@SuppressWarnings("resource")
	public static GenericContainer<?> redis() {
		return new GenericContainer<>("redis:4.0.6").withExposedPorts(6379);
	}

	@SuppressWarnings("resource")
	public static GenericContainer<?> cassandra() {
		return new GenericContainer<>("cassandra:3.11.1").withExposedPorts(9042)
				.waitingFor(new CassandraConnectionVerifyingWaitStrategy());
	}

	public static GenericContainer<?> neo4j() {
		return new GenericContainer<>("neo4j:3.3.1").withExposedPorts(7687)
				.waitingFor(new Neo4jConnectionVerifyingWaitStrategy())
				.withEnv("NEO4J_AUTH", "none");
	}

	private static class CassandraConnectionVerifyingWaitStrategy extends HostPortWaitStrategy {

		@Override
		protected void waitUntilReady() {
			super.waitUntilReady();

			try {
				Unreliables.retryUntilTrue((int) this.startupTimeout.getSeconds(),
						TimeUnit.SECONDS, checkConnection());
			}
			catch (TimeoutException ex) {
				throw new IllegalStateException(ex);
			}
		}

		private Callable<Boolean> checkConnection() {
			return () -> {
				try (Cluster cluster = Cluster.builder().withPort(this.container.getMappedPort(9042))
						.addContactPoint("localhost")
						.build()) {
					cluster.connect();
					return true;
				}
				catch (IllegalArgumentException | NoHostAvailableException ex) {
					return false;
				}
			};
		}

	}

	private static class Neo4jConnectionVerifyingWaitStrategy extends HostPortWaitStrategy {

		@Override
		protected void waitUntilReady() {
			super.waitUntilReady();
			Configuration configuration = new Configuration.Builder()
					.uri("bolt://localhost:" + this.container.getMappedPort(7687)).build();
			SessionFactory sessionFactory = new SessionFactory(configuration,
					"org.springframework.boot.test.autoconfigure.data.neo4j");
			try {
				Unreliables.retryUntilTrue((int) this.startupTimeout.getSeconds(),
						TimeUnit.SECONDS, checkConnection(sessionFactory));
			}
			catch (TimeoutException e) {
				throw new IllegalStateException();
			}
		}

		private Callable<Boolean> checkConnection(SessionFactory sessionFactory) {
			return () -> {
				try {
					sessionFactory.openSession().beginTransaction().close();
					return true;
				}
				catch (Exception ex) {
					return false;
				}
			};
		}
	}

}
