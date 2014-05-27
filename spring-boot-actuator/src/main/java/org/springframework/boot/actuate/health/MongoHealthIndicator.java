/*
 * Copyright 2012-2014 the original author or authors.
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

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.util.Assert;

import com.mongodb.CommandResult;

/**
 * Simple implementation of a {@link HealthIndicator} returning status information for
 * Mongo data stores.
 * 
 * @author Christian Dupuis
 * @since 1.1.0
 */
public class MongoHealthIndicator extends AbstractHealthIndicator {

	private final MongoTemplate mongoTemplate;

	public MongoHealthIndicator(MongoTemplate mongoTemplate) {
		Assert.notNull(mongoTemplate, "MongoTemplate must not be null");
		this.mongoTemplate = mongoTemplate;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		CommandResult result = this.mongoTemplate.executeCommand("{ serverStatus: 1 }");
		builder.up().withDetail("version", result.getString("version"));
	}

}
