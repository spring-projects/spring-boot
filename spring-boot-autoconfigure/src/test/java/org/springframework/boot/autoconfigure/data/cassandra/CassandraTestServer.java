/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.cassandra;

import com.datastax.driver.core.Cluster;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assume;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * {@link TestRule} for working with an optional Cassandra server.
 *
 * @author Stephane Nicoll
 */
public class CassandraTestServer implements TestRule {

	private static final Log logger = LogFactory.getLog(CassandraTestServer.class);

	private Cluster cluster;

	@Override
	public Statement apply(Statement base, Description description) {
		try {
			this.cluster = newCluster();
			return new CassandraStatement(base, this.cluster);
		}
		catch (Exception ex) {
			logger.error("No Cassandra server available", ex);
			return new SkipStatement();
		}
	}

	private Cluster newCluster() {
		Cluster cluster = Cluster.builder().addContactPoint("localhost").build();
		testCluster(cluster);
		return cluster;
	}

	private void testCluster(Cluster cluster) {
		cluster.connect().close();
	}

	/**
	 * @return the cluster if any
	 */
	public Cluster getCluster() {
		return this.cluster;
	}

	private static class CassandraStatement extends Statement {

		private final Statement base;

		private final Cluster cluster;

		CassandraStatement(Statement base, Cluster cluster) {
			this.base = base;
			this.cluster = cluster;
		}

		@Override
		public void evaluate() throws Throwable {
			try {
				this.base.evaluate();
			}
			finally {
				this.cluster.closeAsync();
			}
		}

	}

	private static class SkipStatement extends Statement {

		@Override
		public void evaluate() throws Throwable {
			Assume.assumeTrue("Skipping test due to Cassandra not being available",
					false);
		}

	}

}
