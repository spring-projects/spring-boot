/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.docs.testing.testcontainers.serviceconnections.ssl;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.PemKeyStore;
import org.springframework.boot.testcontainers.service.connection.PemTrustStore;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.RedisOperations;

@Testcontainers
@SpringBootTest
class MyRedisWithSslIntegrationTests {

	@Container
	@ServiceConnection
	@PemKeyStore(certificate = "classpath:client.crt", privateKey = "classpath:client.key")
	@PemTrustStore("classpath:ca.crt")
	static RedisContainer redis = new SecureRedisContainer("redis:latest");

	@Autowired
	@SuppressWarnings("unused")
	private RedisOperations<Object, Object> operations;

	@Test
	void testRedis() {
		// ...
	}

}
