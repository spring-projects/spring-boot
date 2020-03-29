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
package org.springframework.boot.actuate.cassandra;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.Statement;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.health.AbstractReactiveHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.data.cassandra.core.ReactiveCassandraOperations;
import org.springframework.util.Assert;

/**
 * A {@link ReactiveHealthIndicator} for Cassandra.
 *
 * @author Artsiom Yudovin
 * @since 2.1.0
 */
public class CassandraReactiveHealthIndicator extends AbstractReactiveHealthIndicator {

	private static final Statement SELECT = new SimpleStatement("SELECT release_version FROM system.local")
			.setConsistencyLevel(ConsistencyLevel.LOCAL_ONE);

	private final ReactiveCassandraOperations reactiveCassandraOperations;

	/**
	 * Create a new {@link CassandraHealthIndicator} instance.
	 * @param reactiveCassandraOperations the Cassandra operations
	 */
	public CassandraReactiveHealthIndicator(ReactiveCassandraOperations reactiveCassandraOperations) {
		super("Cassandra health check failed");
		Assert.notNull(reactiveCassandraOperations, "ReactiveCassandraOperations must not be null");
		this.reactiveCassandraOperations = reactiveCassandraOperations;
	}

	@Override
	protected Mono<Health> doHealthCheck(Health.Builder builder) {
		return this.reactiveCassandraOperations.getReactiveCqlOperations().queryForObject(SELECT, String.class)
				.map((version) -> builder.up().withDetail("version", version).build()).single();
	}

}
