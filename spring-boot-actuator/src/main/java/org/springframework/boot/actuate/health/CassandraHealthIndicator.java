/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.actuate.health;

import org.springframework.data.cassandra.core.CassandraAdminOperations;
import org.springframework.util.Assert;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;

/**
 * Simple implementation of a {@link HealthIndicator} returning status information for
 * Cassandra data stores.
 *
 * @author Julien Dubois
 * @since 1.3.0
 */
public class CassandraHealthIndicator extends AbstractHealthIndicator {

	private CassandraAdminOperations cassandraAdminOperations;

	/**
	 * Create a new {@link CassandraHealthIndicator} instance.
	 * @param cassandraAdminOperations the Cassandra admin operations
	 */
	public CassandraHealthIndicator(CassandraAdminOperations cassandraAdminOperations) {
		Assert.notNull(cassandraAdminOperations,
				"CassandraAdminOperations must not be null");
		this.cassandraAdminOperations = cassandraAdminOperations;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		try {
			Select select = QueryBuilder.select("release_version").from("system",
					"local");
			ResultSet results = this.cassandraAdminOperations.query(select);
			if (results.isExhausted()) {
				builder.up();
				return;
			}
			String version = results.one().getString(0);
			builder.up().withDetail("version", version);
		}
		catch (Exception ex) {
			builder.down(ex);
		}
	}

}
