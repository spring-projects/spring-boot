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

package org.springframework.boot.actuate.elasticsearch;

import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.health.AbstractReactiveHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient;
import org.springframework.http.HttpStatus;

/**
 * {@link HealthIndicator} for an Elasticsearch cluster using a
 * {@link ReactiveElasticsearchClient}.
 *
 * @author Brian Clozel
 * @author Aleksander Lech
 * @author Scott Frederick
 * @author Peter-Josef Meisch
 * @since 2.3.2
 */
public class ElasticsearchReactiveHealthIndicator extends AbstractReactiveHealthIndicator {

	private final ReactiveElasticsearchClient reactiveElasticsearchClient;

	private final JsonParser jsonParser;

	public ElasticsearchReactiveHealthIndicator(ReactiveElasticsearchClient reactiveElasticsearchClient) {
		super("Elasticsearch health check failed");
		this.reactiveElasticsearchClient = reactiveElasticsearchClient;
		this.jsonParser = JsonParserFactory.getJsonParser();
	}

	@Override
	protected Mono<Health> doHealthCheck(Health.Builder builder) {
		return this.reactiveElasticsearchClient.cluster().health(new ClusterHealthRequest())
				.map((response) -> buildHealth(builder, response))
				.onErrorResume((throwable) -> handleError(builder, throwable));
	}

	private Health buildHealth(Health.Builder builder, ClusterHealthResponse response) {
		return builder.status((response.getStatus() == ClusterHealthStatus.RED) ? Status.OUT_OF_SERVICE : Status.UP)
				.withDetails(this.jsonParser.parseMap(response.toString())).build();
	}

	private Mono<Health> handleError(Health.Builder builder, Throwable throwable) {
		builder.down(throwable);
		if (throwable instanceof ElasticsearchStatusException) {
			HttpStatus status = HttpStatus.resolve(((ElasticsearchStatusException) throwable).status().getStatus());
			if (status != null) {
				builder.withDetail("statusCode", status.value());
				builder.withDetail("reasonPhrase", status.getReasonPhrase());
			}
		}
		return Mono.just(builder.build());
	}

}
