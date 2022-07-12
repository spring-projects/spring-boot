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

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.GetResponse;
import org.junit.jupiter.api.Test;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.testcontainers.DockerImageNames;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ElasticsearchClientAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
@Testcontainers(disabledWithoutDocker = true)
class ElasticsearchClientAutoConfigurationIntegrationTests {

	@Container
	static ElasticsearchContainer elasticsearch = new ElasticsearchContainer(DockerImageNames.elasticsearch())
			.withStartupAttempts(5).withStartupTimeout(Duration.ofMinutes(10));

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class,
					ElasticsearchRestClientAutoConfiguration.class, ElasticsearchClientAutoConfiguration.class));

	@Test
	void reactiveClientCanQueryElasticsearchNode() {
		this.contextRunner
				.withPropertyValues("spring.elasticsearch.uris=" + elasticsearch.getHttpHostAddress(),
						"spring.elasticsearch.connection-timeout=120s", "spring.elasticsearch.socket-timeout=120s")
				.run((context) -> {
					ElasticsearchClient client = context.getBean(ElasticsearchClient.class);
					client.index((b) -> b.index("foo").id("1").document(Map.of("a", "alpha", "b", "bravo")));
					GetResponse<Object> response = client.get((b) -> b.index("foo").id("1"), Object.class);
					assertThat(response).isNotNull();
					assertThat(response.found()).isTrue();
				});
	}

}
