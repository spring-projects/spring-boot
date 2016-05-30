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

package org.springframework.boot.neo4j;

import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.session.SessionFactory;

/**
 * Provide a Neo4j {@link SessionFactory} instance based on a configurable
 * {@link Configuration} and custom packages to scan.
 *
 * @author Stephane Nicoll
 * @since 1.4.0
 * @see NodeEntityScan
 */
public class SessionFactoryProvider {

	private Configuration configuration;

	private String[] packagesToScan;

	/**
	 * Set the configuration to use.
	 * @param configuration the configuration
	 */
	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

	/**
	 * Set the packages to scan.
	 * @param packagesToScan the packages to scan
	 */
	public void setPackagesToScan(String... packagesToScan) {
		this.packagesToScan = packagesToScan;
	}

	public SessionFactory getSessionFactory() {
		return new SessionFactory(this.configuration, this.packagesToScan);
	}

}
