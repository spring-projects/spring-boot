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

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import org.rnorth.ducttape.TimeoutException;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;

/**
 * A {@link GenericContainer} for Cassandra.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 */
public class CassandraContainer extends Container {

	private static final int PORT = 9042;

	public CassandraContainer() {
		super("cassandra:3.11.1", PORT,
				(container) -> container.waitingFor(new WaitStrategy(container))
						.withStartupAttempts(5)
						.withStartupTimeout(Duration.ofSeconds(120)));
	}

	private static final class WaitStrategy extends HostPortWaitStrategy {

		private final GenericContainer<?> container;

		private WaitStrategy(GenericContainer<?> container) {
			this.container = container;
		}

		@Override
		public void waitUntilReady() {
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
				try (Cluster cluster = Cluster.builder().withoutJMXReporting()
						.withPort(this.container.getMappedPort(CassandraContainer.PORT))
						.addContactPoint("localhost").build()) {
					cluster.connect();
					return true;
				}
				catch (IllegalArgumentException | NoHostAvailableException ex) {
					return false;
				}
			};
		}

	}

}
