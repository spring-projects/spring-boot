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

import java.io.InputStream;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.Test;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.testcontainers.DockerImageNames;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ElasticsearchRestClientAutoConfiguration}.
 *
 * @author Brian Clozel
 * @author Vedran Pavic
 * @author Evgeniy Cheban
 * @author Filip Hrisafov
 */
@Testcontainers(disabledWithoutDocker = true)
class ElasticsearchRestClientAutoConfigurationIntegrationTests {

	@Container
	static final ElasticsearchContainer elasticsearch = new ElasticsearchContainer(DockerImageNames.elasticsearch())
			.withStartupAttempts(5).withStartupTimeout(Duration.ofMinutes(10));

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ElasticsearchRestClientAutoConfiguration.class));

	@Test
	@SuppressWarnings("deprecation")
	void restHighLevelClientCanQueryElasticsearchNode() {
		this.contextRunner
				.withPropertyValues("spring.elasticsearch.uris=" + elasticsearch.getHttpHostAddress(),
						"spring.elasticsearch.connection-timeout=120s", "spring.elasticsearch.socket-timeout=120s")
				.run((context) -> {
					org.elasticsearch.client.RestHighLevelClient client = context
							.getBean(org.elasticsearch.client.RestHighLevelClient.class);
					Map<String, String> source = new HashMap<>();
					source.put("a", "alpha");
					source.put("b", "bravo");
					IndexRequest index = new IndexRequest("test").id("1").source(source);
					client.index(index, RequestOptions.DEFAULT);
					GetRequest getRequest = new GetRequest("test").id("1");
					assertThat(client.get(getRequest, RequestOptions.DEFAULT).isExists()).isTrue();
				});
	}

	@Test
	void restClientCanQueryElasticsearchNode() {
		this.contextRunner
				.withPropertyValues("spring.elasticsearch.uris=" + elasticsearch.getHttpHostAddress(),
						"spring.elasticsearch.connection-timeout=120s", "spring.elasticsearch.socket-timeout=120s")
				.run((context) -> {
					RestClient client = context.getBean(RestClient.class);
					Request index = new Request("PUT", "/test/_doc/2");
					index.setJsonEntity("{" + "  \"a\": \"alpha\"," + "  \"b\": \"bravo\"" + "}");
					client.performRequest(index);
					Request getRequest = new Request("GET", "/test/_doc/2");
					Response response = client.performRequest(getRequest);
					try (InputStream input = response.getEntity().getContent()) {
						JsonNode result = new ObjectMapper().readTree(input);
						assertThat(result.path("found").asBoolean()).isTrue();
					}
				});
	}

	@Test
	@SuppressWarnings("deprecation")
	void restClientCanQueryElasticsearchNodeWithoutHighLevelClient() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(org.elasticsearch.client.RestHighLevelClient.class))
				.withPropertyValues("spring.elasticsearch.uris=" + elasticsearch.getHttpHostAddress(),
						"spring.elasticsearch.connection-timeout=120s", "spring.elasticsearch.socket-timeout=120s")
				.run((context) -> {
					RestClient client = context.getBean(RestClient.class);
					Request index = new Request("PUT", "/test/_doc/3");
					index.setJsonEntity("{" + "  \"a\": \"alpha\"," + "  \"b\": \"bravo\"" + "}");
					client.performRequest(index);
					Request getRequest = new Request("GET", "/test/_doc/3");
					Response response = client.performRequest(getRequest);
					try (InputStream input = response.getEntity().getContent()) {
						JsonNode result = new ObjectMapper().readTree(input);
						assertThat(result.path("found").asBoolean()).isTrue();
					}
				});
	}

}
