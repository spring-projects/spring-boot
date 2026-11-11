/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.elasticsearch.test.autoconfigure;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Example repository used with {@link DataElasticsearchTest @DataElasticsearchTest}
 * tests.
 *
 * @author Eddú Meléndez
 */
interface ExampleRepository extends ElasticsearchRepository<ExampleDocument, String> {

}
