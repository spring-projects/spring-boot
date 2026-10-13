/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.mongodb.health;

import java.util.ArrayList;
import java.util.List;

import com.mongodb.client.MongoClient;
import org.bson.Document;

import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.util.Assert;

/**
 * Simple implementation of a {@link HealthIndicator} returning status information for
 * MongoDB.
 *
 * @author Christian Dupuis
 * @author Seonwoo Jung
 * @since 4.0.0
 */
public class MongoHealthIndicator extends AbstractHealthIndicator {

	private static final String ADMIN_DATABASE = "admin";

	private static final Document HELLO_COMMAND = Document.parse("{ hello: 1 }");

	private final MongoClient mongoClient;

	public MongoHealthIndicator(MongoClient mongoClient) {
		super("MongoDB health check failed");
		Assert.notNull(mongoClient, "'mongoClient' must not be null");
		this.mongoClient = mongoClient;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		List<String> databases = new ArrayList<>();
		this.mongoClient.listDatabaseNames().forEach(databases::add);
		Document result = this.mongoClient.getDatabase(getDatabaseName(databases)).runCommand(HELLO_COMMAND);
		builder.up()
			.withDetail("databases", databases)
			.withDetail("maxWireVersion", result.getInteger("maxWireVersion"));
	}

	private static String getDatabaseName(List<String> databases) {
		if (databases.contains(ADMIN_DATABASE)) {
			return ADMIN_DATABASE;
		}
		return (!databases.isEmpty()) ? databases.get(0) : ADMIN_DATABASE;
	}

}
