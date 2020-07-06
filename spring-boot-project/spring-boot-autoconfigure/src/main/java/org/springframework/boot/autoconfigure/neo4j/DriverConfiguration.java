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

package org.springframework.boot.autoconfigure.neo4j;

import org.neo4j.driver.AuthToken;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Creates an instance of the Neo4j Java driver.
 *
 * @author Michael J. Simons
 */
@Configuration(proxyBeanMethods = false)
final class DriverConfiguration {

	@Bean
	@ConditionalOnMissingBean(Driver.class)
	Driver neo4jDriver(final Neo4jDriverProperties driverProperties) {

		final AuthToken authToken = driverProperties.getAuthentication().asAuthToken();
		final Config config = driverProperties.asDriverConfig();

		return GraphDatabase.driver(driverProperties.getUri(), authToken, config);
	}

}
