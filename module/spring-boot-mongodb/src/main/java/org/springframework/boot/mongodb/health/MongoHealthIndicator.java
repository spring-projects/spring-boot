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
 * @since 4.0.0
 */
public class MongoHealthIndicator extends AbstractHealthIndicator {

	private static final Document HELLO_COMMAND = Document.parse("{ hello: 1 }");

	private final MongoClient mongoClient;

	public MongoHealthIndicator(MongoClient mongoClient) {
		super("MongoDB health check failed");
		Assert.notNull(mongoClient, "'mongoClient' must not be null");
		this.mongoClient = mongoClient;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		Map<String, Object> details = new LinkedHashMap<>();
		List<String> databases = new ArrayList<>();
		details.put("databases", databases);
		this.mongoClient.listDatabaseNames().forEach((database) -> {
			Document result = this.mongoClient.getDatabase(database).runCommand(HELLO_COMMAND);
			databases.add(database);
			details.putIfAbsent("maxWireVersion", result.getInteger("maxWireVersion"));
		});
		builder.up().withDetails(details);
	}

}
