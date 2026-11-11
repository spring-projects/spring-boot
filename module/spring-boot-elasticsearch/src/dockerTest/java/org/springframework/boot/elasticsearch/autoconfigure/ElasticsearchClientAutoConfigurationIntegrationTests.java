/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.elasticsearch.autoconfigure;

import java.util.Map;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.GetResponse;
import org.junit.jupiter.api.Test;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.container.TestImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ElasticsearchClientAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
@Testcontainers(disabledWithoutDocker = true)
class ElasticsearchClientAutoConfigurationIntegrationTests {

	@Container
	static final ElasticsearchContainer elasticsearch = TestImage.ELASTICSEARCH.container();

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
