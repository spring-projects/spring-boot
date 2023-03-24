/*
 * Copyright 2012-2023 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.exceptions.SessionExpiredException;
import org.neo4j.driver.reactivestreams.ReactiveResult;
import org.neo4j.driver.reactivestreams.ReactiveSession;
import org.neo4j.driver.summary.ResultSummary;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import org.springframework.boot.actuate.health.AbstractReactiveHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;

/**
 * {@link ReactiveHealthIndicator} that tests the status of a Neo4j by executing a Cypher
 * statement and extracting server and database information.
 *
 * @author Michael J. Simons
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 2.4.0
 */
public final class Neo4jReactiveHealthIndicator extends AbstractReactiveHealthIndicator {

	private static final Log logger = LogFactory.getLog(Neo4jReactiveHealthIndicator.class);

	private final Driver driver;

	private final Neo4jHealthDetailsHandler healthDetailsHandler;

	public Neo4jReactiveHealthIndicator(Driver driver) {
		this.driver = driver;
		this.healthDetailsHandler = new Neo4jHealthDetailsHandler();
	}

	@Override
	protected Mono<Health> doHealthCheck(Health.Builder builder) {
		return runHealthCheckQuery()
			.doOnError(SessionExpiredException.class, (ex) -> logger.warn(Neo4jHealthIndicator.MESSAGE_SESSION_EXPIRED))
			.retryWhen(Retry.max(1).filter(SessionExpiredException.class::isInstance))
			.map((healthDetails) -> {
				this.healthDetailsHandler.addHealthDetails(builder, healthDetails);
				return builder.build();
			});
	}

	Mono<Neo4jHealthDetails> runHealthCheckQuery() {
		return Mono.using(this::session, this::healthDetails, ReactiveSession::close);
	}

	private ReactiveSession session() {
		return this.driver.session(ReactiveSession.class, Neo4jHealthIndicator.DEFAULT_SESSION_CONFIG);
	}

	private Mono<Neo4jHealthDetails> healthDetails(ReactiveSession session) {
		return Mono.from(session.run(Neo4jHealthIndicator.CYPHER)).flatMap(this::healthDetails);
	}

	private Mono<? extends Neo4jHealthDetails> healthDetails(ReactiveResult result) {
		Flux<Record> records = Flux.from(result.records());
		Mono<ResultSummary> summary = Mono.from(result.consume());
		Neo4jHealthDetailsBuilder builder = new Neo4jHealthDetailsBuilder();
		return records.single().doOnNext(builder::record).then(summary).map(builder::build);
	}

	/**
	 * Builder used to create a {@link Neo4jHealthDetails} from a {@link Record} and a
	 * {@link ResultSummary}.
	 */
	private static class Neo4jHealthDetailsBuilder {

		private Record record;

		void record(Record record) {
			this.record = record;
		}

		private Neo4jHealthDetails build(ResultSummary summary) {
			return new Neo4jHealthDetails(this.record, summary);
		}

	}

}
