/*
 * Copyright 2014 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.mongodb.core.MongoTemplate;

import com.mongodb.CommandResult;

/**
 * Simple implementation of a {@link HealthIndicator} returning status information for
 * Mongo data stores.
 *
 * @author Christian Dupuis
 * @since 1.1.0
 */
public class MongoHealthIndicator implements HealthIndicator<Map<String, Object>> {

	private final MongoTemplate mongoTemplate;

	public MongoHealthIndicator(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	@Override
	public Map<String, Object> health() {
		Map<String, Object> health = new HashMap<String, Object>();
		try {
			CommandResult result = this.mongoTemplate
					.executeCommand("{ serverStatus: 1 }");
			health.put("status", "ok");
			health.put("version", result.getString("version"));
		}
		catch (Exception ex) {
			health.put("status", "error");
			health.put("error", ex.getClass().getName() + ": " + ex.getMessage());
		}
		return health;
	}

}
