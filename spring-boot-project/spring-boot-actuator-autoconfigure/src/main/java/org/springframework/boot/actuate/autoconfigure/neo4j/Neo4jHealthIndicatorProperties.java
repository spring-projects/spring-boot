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

package org.springframework.boot.actuate.autoconfigure.neo4j;

import org.springframework.boot.actuate.neo4j.Neo4jHealthIndicator;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

/**
 * External configuration properties for {@link Neo4jHealthIndicator}.
 *
 * @author Eric Spiegelberg
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "management.health.neo4j", ignoreUnknownFields = false)
public class Neo4jHealthIndicatorProperties {

	private static final String DEFAULT_CYPHER = "match (n) return count(n) as nodes";

	/**
	 * The Cypher statement used to verify Neo4j is up and available.
	 */
	private String cypher = DEFAULT_CYPHER;

	public String getCypher() {
		return this.cypher;
	}

	public void setCypher(String cypher) {
		Assert.hasText(cypher, "cypher must have text");
		this.cypher = cypher;
	}

}
