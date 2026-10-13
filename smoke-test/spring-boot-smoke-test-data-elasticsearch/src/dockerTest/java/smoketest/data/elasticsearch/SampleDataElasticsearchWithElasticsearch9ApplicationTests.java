/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.elasticsearch;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.elasticsearch.test.autoconfigure.DataElasticsearchTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.Ssl;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests for Data Elasticsearch with Elasticsearch 9.
 *
 * @author Moritz Halbritter
 * @author Stephane Nicoll
 */
@Testcontainers(disabledWithoutDocker = true)
@DataElasticsearchTest
class SampleDataElasticsearchWithElasticsearch9ApplicationTests {

	@Container
	@ServiceConnection
	@Ssl
	static final ElasticsearchContainer elasticSearch = new ElasticsearchContainer(TestImage.ELASTICSEARCH_9.toString())
		.withPassword("my-custom-password");

	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;

	@Autowired
	private SampleRepository exampleRepository;

	@Test
	void testRepository() {
		SampleDocument document = new SampleDocument();
		document.setText("Look, new @DataElasticsearchTest!");
		String id = UUID.randomUUID().toString();
		document.setId(id);
		SampleDocument savedDocument = this.exampleRepository.save(document);
		SampleDocument getDocument = this.elasticsearchTemplate.get(id, SampleDocument.class);
		assertThat(getDocument).isNotNull();
		assertThat(getDocument.getId()).isNotNull();
		assertThat(getDocument.getId()).isEqualTo(savedDocument.getId());
		this.exampleRepository.deleteAll();
	}

}
