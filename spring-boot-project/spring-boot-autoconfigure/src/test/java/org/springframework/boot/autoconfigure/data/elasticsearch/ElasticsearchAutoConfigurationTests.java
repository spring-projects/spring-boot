/*
 * Copyright 2012-2019 the original author or authors.
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
package org.springframework.boot.autoconfigure.data.elasticsearch;

import java.util.List;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;

import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.testsupport.testcontainers.DisabledWithoutDockerTestcontainers;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ElasticsearchAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
@DisabledWithoutDockerTestcontainers
class ElasticsearchAutoConfigurationTests {

	@Container
	public static ElasticsearchContainer elasticsearch = new ElasticsearchContainer();

	private AnnotationConfigApplicationContext context;

	@BeforeEach
	public void setUp() {
		System.setProperty("es.set.netty.runtime.available.processors", "false");
	}

	@AfterEach
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
		System.clearProperty("es.set.netty.runtime.available.processors");
	}

	@Test
	void useExistingClient() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(CustomConfiguration.class, PropertyPlaceholderAutoConfiguration.class,
				ElasticsearchAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBeanNamesForType(Client.class).length).isEqualTo(1);
		assertThat(this.context.getBean("myClient")).isSameAs(this.context.getBean(Client.class));
	}

	@Test
	void createTransportClient() {
		this.context = new AnnotationConfigApplicationContext();
		TestPropertyValues
				.of("spring.data.elasticsearch.cluster-nodes:" + elasticsearch.getTcpHost().getHostString() + ":"
						+ elasticsearch.getTcpHost().getPort(), "spring.data.elasticsearch.cluster-name:docker-cluster")
				.applyTo(this.context);
		this.context.register(PropertyPlaceholderAutoConfiguration.class, ElasticsearchAutoConfiguration.class);
		this.context.refresh();
		List<DiscoveryNode> connectedNodes = this.context.getBean(TransportClient.class).connectedNodes();
		assertThat(connectedNodes).hasSize(1);
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomConfiguration {

		@Bean
		public Client myClient() {
			return mock(Client.class);
		}

	}

}
