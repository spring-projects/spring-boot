/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.testing.springbootapplications.autoconfiguredspringdataelasticsearch

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.elasticsearch.test.autoconfigure.DataElasticsearchTest

@DataElasticsearchTest
class MyDataElasticsearchTests(@Autowired val repository: SomeRepository) {

	// ...

}

