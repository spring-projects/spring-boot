/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.testing.testcontainers.serviceconnections.ssl;

import com.redis.testcontainers.RedisContainer;

class SecureRedisContainer extends RedisContainer {

	SecureRedisContainer(String dockerImageName) {
		super(dockerImageName);
	}

}
