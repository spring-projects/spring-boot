/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.actuate.health;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.indices.Stats;

/**
 * {@link HealthIndicator} for Elasticsearch using a {@link JestClient}.
 *
 * @author Stephane Nicoll
 * @since 1.4.0
 */
public class ElasticsearchJestHealthIndicator extends AbstractHealthIndicator {

	private final JestClient jestClient;

	private final JsonParser jsonParser = new JsonParser();

	public ElasticsearchJestHealthIndicator(JestClient jestClient) {
		this.jestClient = jestClient;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		JestResult aliases = this.jestClient.execute(new Stats.Builder().build());
		JsonElement root = this.jsonParser.parse(aliases.getJsonString());
		JsonObject shards = root.getAsJsonObject().get("_shards").getAsJsonObject();
		int failedShards = shards.get("failed").getAsInt();
		if (failedShards != 0) {
			builder.outOfService();
		}
		else {
			builder.up();
		}
	}

}
