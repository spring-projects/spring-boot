/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.couchbase;

import java.util.concurrent.TimeUnit;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assume;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * {@link TestRule} for working with an optional Couchbase server. Expects
 * a default {@link Bucket} with no password to be available on localhost.
 *
 * @author Stephane Nicoll
 */
public class CouchbaseTestServer implements TestRule {

	private static final Log logger = LogFactory.getLog(CouchbaseTestServer.class);

	private CouchbaseEnvironment env;

	private Cluster cluster;

	@Override
	public Statement apply(Statement base, Description description) {
		try {
			this.env = DefaultCouchbaseEnvironment.create();
			this.cluster = CouchbaseCluster.create(this.env, "localhost");
			testConnection(this.cluster);
			return new CouchbaseStatement(base, this.env, this.cluster);
		}
		catch (Exception e) {
			logger.info("No couchbase server available");
			return new SkipStatement();
		}
	}

	private static void testConnection(Cluster cluster) {
		Bucket bucket = cluster.openBucket(2, TimeUnit.SECONDS);
		bucket.close();
	}

	/**
	 * @return the couchbase env if any
	 */
	public CouchbaseEnvironment getEnv() {
		return this.env;
	}

	/**
	 * @return the cluster if any
	 */
	public Cluster getCluster() {
		return this.cluster;
	}


	private static class CouchbaseStatement extends Statement {
		private final Statement base;
		private final CouchbaseEnvironment env;

		private final Cluster cluster;

		CouchbaseStatement(Statement base, CouchbaseEnvironment env, Cluster cluster) {
			this.base = base;
			this.env = env;
			this.cluster = cluster;
		}

		@Override
		public void evaluate() throws Throwable {
			try {
				this.base.evaluate();
			}
			finally {
				try {
					this.cluster.disconnect();
					this.env.shutdownAsync();
				}
				catch (Exception ex) {
					logger.warn("Exception while trying to cleanup couchbase resource", ex);
				}
			}
		}
	}

	private static class SkipStatement extends Statement {

		@Override
		public void evaluate() throws Throwable {
			Assume.assumeTrue("Skipping test due to Couchbase "
					+ "not being available", false);
		}

	}

}
