/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.mongodb.health;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mongodb.reactivestreams.client.MongoClient;
import org.bson.Document;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.boot.health.contributor.AbstractReactiveHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.ReactiveHealthIndicator;
import org.springframework.util.Assert;

/**
 * A {@link ReactiveHealthIndicator} for Mongo.
 *
 * @author Yulin Qin
 * @since 4.0.0
 */
public class MongoReactiveHealthIndicator extends AbstractReactiveHealthIndicator {

	private static final Document HELLO_COMMAND = Document.parse("{ hello: 1 }");

	private final MongoClient mongoClient;

	public MongoReactiveHealthIndicator(MongoClient mongoClient) {
		super("Mongo health check failed");
		Assert.notNull(mongoClient, "'mongoClient' must not be null");
		this.mongoClient = mongoClient;
	}

	@Override
	protected Mono<Health> doHealthCheck(Health.Builder builder) {
		Mono<Map<String, Object>> healthDetails = Flux.from(this.mongoClient.listDatabaseNames())
			.flatMap((database) -> Mono.from(this.mongoClient.getDatabase(database).runCommand(HELLO_COMMAND))
				.map((document) -> new HelloResponse(database, document)))
			.collectList()
			.map((responses) -> {
				Map<String, Object> databaseDetails = new LinkedHashMap<>();
				List<String> databases = new ArrayList<>();
				databaseDetails.put("databases", databases);
				for (HelloResponse response : responses) {
					databases.add(response.database());
					databaseDetails.putIfAbsent("maxWireVersion", response.document().getInteger("maxWireVersion"));
				}
				return databaseDetails;
			});
		return healthDetails.map((details) -> builder.up().withDetails(details).build());
	}

	private record HelloResponse(String database, Document document) {
	}

}
