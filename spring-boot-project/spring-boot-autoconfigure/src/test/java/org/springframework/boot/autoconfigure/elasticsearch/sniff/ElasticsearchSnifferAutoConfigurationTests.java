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

package org.springframework.boot.autoconfigure.elasticsearch.sniff;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.*;
import org.elasticsearch.client.sniff.ElasticsearchNodesSniffer;
import org.elasticsearch.client.sniff.NodesSniffer;
import org.elasticsearch.client.sniff.Sniffer;
import org.elasticsearch.client.sniff.SnifferBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.sniff.SnifferBuilderCustomizer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.testcontainers.DockerImageNames;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link org.springframework.boot.autoconfigure.elasticsearch.sniff.ElasticsearchSnifferAutoConfiguration}.
 *
 * @author Asha Somayajula
 */
@Testcontainers(disabledWithoutDocker = true)
class ElasticsearchSnifferAutoConfigurationTests {

	@Container
	static final ElasticsearchContainer elasticsearch = new ElasticsearchContainer(DockerImageNames.elasticsearch())
			.withStartupAttempts(5).withStartupTimeout(Duration.ofMinutes(10));

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ElasticsearchRestClientAutoConfiguration.class));

	@Test
	void configureShouldOnlyCreateSnifferInstance() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(RestClient.class)
				.hasSingleBean(RestHighLevelClient.class));
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(Sniffer.class));
	}

	@Configuration(proxyBeanMethods = false)
	static class BuilderCustomizerConfiguration {

		@Bean
		SnifferBuilderCustomizer myCustomizer() {
			return new SnifferBuilderCustomizer() {

				@Override
				public void customize(SnifferBuilder builder) {
					builder.setSniffAfterFailureDelayMillis(100);
					builder.setSniffIntervalMillis(100);
					NodesSniffer nodesSniffer = new NodesSniffer() {
						@Override
						public List<Node> sniff() throws IOException {
							return null;
						}
					};

					builder.setNodesSniffer(nodesSniffer);
				}
			};
			};
		}
	}
