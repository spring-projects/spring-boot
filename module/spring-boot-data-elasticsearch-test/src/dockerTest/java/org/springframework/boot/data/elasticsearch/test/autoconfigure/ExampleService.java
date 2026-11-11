/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.elasticsearch.test.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.stereotype.Service;

/**
 * Example service used with {@link DataElasticsearchTest @DataElasticsearchTest} tests.
 *
 * @author Eddú Meléndez
 */
@Service
public class ExampleService {

	private final ElasticsearchTemplate elasticsearchTemplate;

	public ExampleService(ElasticsearchTemplate elasticsearchRestTemplate) {
		this.elasticsearchTemplate = elasticsearchRestTemplate;
	}

	public @Nullable ExampleDocument findById(String id) {
		return this.elasticsearchTemplate.get(id, ExampleDocument.class);
	}

}
