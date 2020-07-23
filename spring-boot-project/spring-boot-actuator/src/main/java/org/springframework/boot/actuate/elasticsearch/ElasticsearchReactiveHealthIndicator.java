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

import java.util.Map;

import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.health.AbstractReactiveHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * {@link HealthIndicator} for an Elasticsearch cluster using a
 * {@link ReactiveElasticsearchClient}.
 *
 * @author Brian Clozel
 * @author Aleksander Lech
 * @since 2.3.2
 */
public class ElasticsearchReactiveHealthIndicator extends AbstractReactiveHealthIndicator {

	private static final ParameterizedTypeReference<Map<String, Object>> STRING_OBJECT_MAP = new ParameterizedTypeReference<Map<String, Object>>() {
	};

	private static final String RED_STATUS = "red";

	private final ReactiveElasticsearchClient client;

	public ElasticsearchReactiveHealthIndicator(ReactiveElasticsearchClient client) {
		super("Elasticsearch health check failed");
		this.client = client;
	}

	@Override
	protected Mono<Health> doHealthCheck(Health.Builder builder) {
		return this.client.execute(this::getHealth).flatMap((response) -> doHealthCheck(builder, response));
	}

	private Mono<ClientResponse> getHealth(WebClient webClient) {
		return webClient.get().uri("/_cluster/health/").exchange();
	}

	private Mono<Health> doHealthCheck(Health.Builder builder, ClientResponse response) {
		if (response.statusCode().is2xxSuccessful()) {
			return response.bodyToMono(STRING_OBJECT_MAP).map((body) -> getHealth(builder, body));
		}
		builder.down();
		builder.withDetail("statusCode", response.rawStatusCode());
		builder.withDetail("reasonPhrase", response.statusCode().getReasonPhrase());
		return response.releaseBody().thenReturn(builder.build());
	}

	private Health getHealth(Health.Builder builder, Map<String, Object> body) {
		String status = (String) body.get("status");
		builder.status(RED_STATUS.equals(status) ? Status.OUT_OF_SERVICE : Status.UP);
		builder.withDetails(body);
		return builder.build();
	}

}
