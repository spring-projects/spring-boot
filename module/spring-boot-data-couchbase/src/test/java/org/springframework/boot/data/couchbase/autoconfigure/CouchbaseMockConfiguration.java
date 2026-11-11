/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.couchbase.autoconfigure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.couchbase.CouchbaseClientFactory;

import static org.mockito.Mockito.mock;

/**
 * Test configuration that mocks access to Couchbase.
 *
 * @author Stephane Nicoll
 */
@Configuration(proxyBeanMethods = false)
class CouchbaseMockConfiguration {

	@Bean
	CouchbaseClientFactory couchbaseClientFactory() {
		return mock(CouchbaseClientFactory.class);
	}

}
