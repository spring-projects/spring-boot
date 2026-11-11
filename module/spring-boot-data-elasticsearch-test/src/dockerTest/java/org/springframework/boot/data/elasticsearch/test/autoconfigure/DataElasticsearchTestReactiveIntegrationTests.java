/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.elasticsearch.test.autoconfigure;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sample tests for {@link DataElasticsearchTest @DataElasticsearchTest} using reactive
 * repositories.
 *
 * @author Eddú Meléndez
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
@DataElasticsearchTest
@Testcontainers(disabledWithoutDocker = true)
class DataElasticsearchTestReactiveIntegrationTests {

	@Container
	@ServiceConnection
	static final ElasticsearchContainer elasticsearch = TestImage.ELASTICSEARCH.container();

	@Autowired
	private ReactiveElasticsearchTemplate elasticsearchTemplate;

	@Autowired
	private ExampleReactiveRepository exampleReactiveRepository;

	@Test
	void testRepository() {
		ExampleDocument exampleDocument = new ExampleDocument();
		exampleDocument.setText("Look, new @DataElasticsearchTest!");
		exampleDocument = this.exampleReactiveRepository.save(exampleDocument).block(Duration.ofSeconds(30));
		assertThat(exampleDocument).isNotNull();
		assertThat(exampleDocument.getId()).isNotNull();
		assertThat(this.elasticsearchTemplate.exists(exampleDocument.getId(), ExampleDocument.class)
			.block(Duration.ofSeconds(30))).isTrue();
	}

}
