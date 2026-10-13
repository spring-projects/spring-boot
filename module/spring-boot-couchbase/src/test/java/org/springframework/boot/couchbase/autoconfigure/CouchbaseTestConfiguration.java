/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.couchbase.autoconfigure;

import com.couchbase.client.core.env.Authenticator;
import com.couchbase.client.java.Cluster;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.Mockito.mock;

/**
 * Test configuration for couchbase that mocks access.
 *
 * @author Stephane Nicoll
 * @author Scott Frederick
 */
@Configuration(proxyBeanMethods = false)
class CouchbaseTestConfiguration {

	private final Cluster cluster = mock(Cluster.class);

	private final Authenticator authenticator = mock(Authenticator.class);

	@Bean
	Cluster couchbaseCluster() {
		return this.cluster;
	}

	@Bean
	Authenticator couchbaseAuth() {
		return this.authenticator;
	}

}
