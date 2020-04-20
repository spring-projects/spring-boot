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

import java.util.stream.Collectors;

import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.health.AbstractReactiveHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient;

/**
 * {@link HealthIndicator} for an Elasticsearch cluster using a
 * {@link ReactiveElasticsearchClient}.
 *
 * @author Aleksander Lech
 * @since 2.3
 */
public class ElasticsearchReactiveHealthIndicator extends AbstractReactiveHealthIndicator {

	private final ReactiveElasticsearchClient client;

	public ElasticsearchReactiveHealthIndicator(ReactiveElasticsearchClient client) {
		super("Elasticsearch health check failed");
		this.client = client;
	}

	@Override
	protected Mono<Health> doHealthCheck(Health.Builder builder) {
		return this.client.status().map((status) -> {
			if (status.isOk()) {
				builder.up();
			}
			else {
				builder.down();
			}

			builder.withDetails(status.hosts().stream().collect(Collectors
					.toMap((host) -> host.getEndpoint().getHostString(), (host) -> host.getState().toString())));

			return builder.build();
		});
	}

}
