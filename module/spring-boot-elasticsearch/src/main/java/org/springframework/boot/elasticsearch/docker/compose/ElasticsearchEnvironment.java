/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.elasticsearch.docker.compose;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Elasticsearch environment details.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ElasticsearchEnvironment {

	private final @Nullable String password;

	ElasticsearchEnvironment(Map<String, @Nullable String> env) {
		Assert.state(!env.containsKey("ELASTIC_PASSWORD_FILE"), "ELASTIC_PASSWORD_FILE is not supported");
		this.password = env.get("ELASTIC_PASSWORD");
	}

	@Nullable String getPassword() {
		return this.password;
	}

}
