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

import org.neo4j.driver.summary.DatabaseInfo;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.summary.ServerInfo;

import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.util.StringUtils;

/**
 * Handle health check details for a Neo4j server.
 *
 * @author Stephane Nicoll
 */
class Neo4jHealthDetailsHandler {

	/**
	 * Add health details for the specified {@link ResultSummary} and {@code edition}.
	 * @param builder the {@link Builder} to use
	 * @param edition the edition of the server
	 * @param resultSummary server information
	 */
	void addHealthDetails(Builder builder, String edition, ResultSummary resultSummary) {
		ServerInfo serverInfo = resultSummary.server();
		builder.up().withDetail("server", serverInfo.version() + "@" + serverInfo.address()).withDetail("edition",
				edition);
		DatabaseInfo databaseInfo = resultSummary.database();
		if (StringUtils.hasText(databaseInfo.name())) {
			builder.withDetail("database", databaseInfo.name());
		}
	}

}
