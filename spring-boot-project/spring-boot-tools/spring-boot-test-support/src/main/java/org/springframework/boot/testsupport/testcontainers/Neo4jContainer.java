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

import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.session.SessionFactory;
import org.rnorth.ducttape.TimeoutException;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.HostPortWaitStrategy;

/**
 * A {@link GenericContainer} for Neo4J.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 */
public class Neo4jContainer extends Container {

	public Neo4jContainer() {
		super("neo4j:3.3.1", 7687, (container) -> container.waitingFor(new WaitStrategy())
				.withEnv("NEO4J_AUTH", "none"));
	}

	private static class WaitStrategy extends HostPortWaitStrategy {

		@Override
		public void waitUntilReady(GenericContainer container) {
			super.waitUntilReady();
			Configuration configuration = new Configuration.Builder()
					.uri("bolt://localhost:" + container.getMappedPort(7687))
					.build();
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
