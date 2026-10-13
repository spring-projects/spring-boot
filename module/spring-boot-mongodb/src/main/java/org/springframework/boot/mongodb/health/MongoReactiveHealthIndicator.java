/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.mongodb.health;

import java.util.List;

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
 * @author Seonwoo Jung
 * @since 4.0.0
 */
public class MongoReactiveHealthIndicator extends AbstractReactiveHealthIndicator {

	private static final String ADMIN_DATABASE = "admin";

	private static final Document HELLO_COMMAND = Document.parse("{ hello: 1 }");

	private final MongoClient mongoClient;

	public MongoReactiveHealthIndicator(MongoClient mongoClient) {
		super("Mongo health check failed");
		Assert.notNull(mongoClient, "'mongoClient' must not be null");
		this.mongoClient = mongoClient;
	}

	@Override
	protected Mono<Health> doHealthCheck(Health.Builder builder) {
		Mono<List<String>> databases = Flux.from(this.mongoClient.listDatabaseNames()).collectList();
		return databases.flatMap((databaseNames) -> Mono
			.from(this.mongoClient.getDatabase(getDatabaseName(databaseNames)).runCommand(HELLO_COMMAND))
			.map((result) -> builder.up()
				.withDetail("databases", databaseNames)
				.withDetail("maxWireVersion", result.getInteger("maxWireVersion"))
				.build()));
	}

	private static String getDatabaseName(List<String> databases) {
		if (databases.contains(ADMIN_DATABASE)) {
			return ADMIN_DATABASE;
		}
		return (!databases.isEmpty()) ? databases.get(0) : ADMIN_DATABASE;
	}

}
