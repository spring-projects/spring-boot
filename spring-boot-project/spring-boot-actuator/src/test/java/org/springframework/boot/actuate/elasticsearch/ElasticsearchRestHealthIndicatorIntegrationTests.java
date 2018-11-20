/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.elasticsearch;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ElasticsearchRestHealthIndicator}.
 *
 * @author Artsiom Yudovin
 */
public class ElasticsearchRestHealthIndicatorIntegrationTests {

	@ClassRule
	public static final ElasticsearchContainer container = new ElasticsearchContainer(
			"docker.elastic.co/elasticsearch/elasticsearch:6.4.1");

	private ElasticsearchRestHealthIndicator elasticsearchRestHealthIndicator;

	@BeforeClass
	public static void start() {
		container.start();
	}

	@AfterClass
	public static void stop() {
		container.stop();
	}

	@Before
	public void initClient() {
		final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
		credentialsProvider.setCredentials(AuthScope.ANY,
				new UsernamePasswordCredentials("elastic", "changeme"));
		RestClient client = RestClient
				.builder(HttpHost.create(container.getHttpHostAddress()))
				.setHttpClientConfigCallback((httpClientBuilder) -> httpClientBuilder
						.setDefaultCredentialsProvider(credentialsProvider))
				.build();
		this.elasticsearchRestHealthIndicator = new ElasticsearchRestHealthIndicator(
				client);
	}

	@Test
	public void elasticsearchIsUp() {
		Health health = this.elasticsearchRestHealthIndicator.health();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
	}

}
