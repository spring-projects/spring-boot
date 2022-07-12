/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.elasticsearch;

import java.time.Duration;
import java.util.Map;

import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import org.junit.jupiter.api.Test;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.testcontainers.DockerImageNames;
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ReactiveElasticsearchClientAutoConfiguration}.
 *
 * @author Brian Clozel
 * @author Andy Wilkinson
 */
@Testcontainers(disabledWithoutDocker = true)
class ReactiveElasticsearchClientAutoConfigurationIntegrationTests {

	@Container
	static ElasticsearchContainer elasticsearch = new ElasticsearchContainer(DockerImageNames.elasticsearch())
			.withStartupAttempts(5).withStartupTimeout(Duration.ofMinutes(10));

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(JacksonAutoConfiguration.class, ElasticsearchRestClientAutoConfiguration.class,
					ReactiveElasticsearchClientAutoConfiguration.class));

	@Test
	void reactiveClientCanQueryElasticsearchNode() {
		this.contextRunner
				.withPropertyValues("spring.elasticsearch.uris=" + elasticsearch.getHttpHostAddress(),
						"spring.elasticsearch.connection-timeout=120s", "spring.elasticsearch.socket-timeout=120s")
				.run((context) -> {
					ReactiveElasticsearchClient client = context.getBean(ReactiveElasticsearchClient.class);
					Mono<IndexResponse> index = client
							.index((b) -> b.index("foo").id("1").document(Map.of("a", "alpha", "b", "bravo")));
					index.block();
					Mono<GetResponse<Object>> get = client.get((b) -> b.index("foo").id("1"), Object.class);
					GetResponse<Object> response = get.block();
					assertThat(response).isNotNull();
					assertThat(response.found()).isTrue();
				});
	}

}
