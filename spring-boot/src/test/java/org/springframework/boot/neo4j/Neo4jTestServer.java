/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.neo4j;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assume;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.config.DriverConfiguration;
import org.neo4j.ogm.session.SessionFactory;

/**
 * {@link TestRule} for working with an optional Neo4j server.
 *
 * @author Eddú Meléndez
 */
public class Neo4jTestServer implements TestRule {

	private static final Log logger = LogFactory.getLog(Neo4jTestServer.class);

	private SessionFactory sessionFactory;

	private String[] packages;

	public Neo4jTestServer(String[] packages) {
		this.packages = packages;
	}

	@Override
	public Statement apply(Statement base, Description description) {
		try {
			this.sessionFactory = createSessionFactory();
			return new Neo4jStatement(base, this.sessionFactory);
		}
		catch (Exception ex) {
			logger.error("No Neo4j server available", ex);
			return new SkipStatement();
		}
	}

	private SessionFactory createSessionFactory() {
		Configuration configuration = new Configuration();
		DriverConfiguration driverConfiguration = configuration.driverConfiguration();
		driverConfiguration.setDriverClassName("org.neo4j.ogm.drivers.http.driver.HttpDriver");
		driverConfiguration.setURI("http://localhost:7474");

		SessionFactory sessionFactory = new SessionFactory(configuration, this.packages);
		testConnection(sessionFactory);
		return sessionFactory;
	}

	private void testConnection(SessionFactory sessionFactory) {
		sessionFactory.openSession().beginTransaction().close();
	}

	private static class Neo4jStatement extends Statement {

		private final Statement base;

		private final SessionFactory sessionFactory;

		Neo4jStatement(Statement base, SessionFactory sessionFactory) {
			this.base = base;
			this.sessionFactory = sessionFactory;
		}

		@Override
		public void evaluate() throws Throwable {
			try {
				this.base.evaluate();
			}
			finally {
				try {
					this.sessionFactory.close();
				}
				catch (Exception ex) {
					logger.warn("Exception while trying to cleanup neo4j resource", ex);
				}
			}
		}

	}

	private static class SkipStatement extends Statement {

		@Override
		public void evaluate() throws Throwable {
			Assume.assumeTrue("Skipping test due to Neo4j SessionFactory"
					+ " not being available", false);
		}

	}

}
