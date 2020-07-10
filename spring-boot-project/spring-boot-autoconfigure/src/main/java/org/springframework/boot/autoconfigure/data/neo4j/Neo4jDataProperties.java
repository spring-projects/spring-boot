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

package org.springframework.boot.autoconfigure.data.neo4j;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;

/**
 * Configuration properties for Spring Data Neo4j.
 *
 * @author Michael J. Simons
 * @since 2.4.0
 */
@ConfigurationProperties(prefix = "spring.data.neo4j")
public class Neo4jDataProperties {

	/**
	 * A statically configured database name. This property is only applicable when
	 * connecting against a 4.0 cluster or server and will lead to errors if used with a
	 * prior version of Neo4j. Leave this null (the default) to indicate that you like the
	 * server to decide the default database to use. The database name set here will be
	 * statically used throughout the lifetime of the application. If you need more
	 * flexibility you can declare a bean of type {@link DatabaseSelectionProvider} which
	 * can for example use Spring's Security Context or similar to determine the current
	 * principal on which you could decide which database to use.
	 */
	private String database;

	public String getDatabase() {
		return this.database;
	}

	public void setDatabase(String database) {
		this.database = database;
	}

}
