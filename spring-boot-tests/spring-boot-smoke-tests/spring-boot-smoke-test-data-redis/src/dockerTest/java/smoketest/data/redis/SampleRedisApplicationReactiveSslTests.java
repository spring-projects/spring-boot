/*
 * Copyright 2012-2025 the original author or authors.
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

package smoketest.data.redis;

import java.time.Duration;
import java.util.UUID;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.PemKeyStore;
import org.springframework.boot.testcontainers.service.connection.PemTrustStore;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.data.redis.core.ReactiveRedisOperations;

/**
 * Smoke tests for Redis using reactive operations and SSL.
 *
 * @author Scott Frederick
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class SampleRedisApplicationReactiveSslTests {

	@Container
	@ServiceConnection
	@PemKeyStore(certificate = "classpath:ssl/test-client.crt", privateKey = "classpath:ssl/test-client.key")
	@PemTrustStore("classpath:ssl/test-ca.crt")
	static RedisContainer redis = TestImage.container(SecureRedisContainer.class);

	@Autowired
	private ReactiveRedisOperations<Object, Object> operations;

	@Test
	void testRepository() {
		String id = UUID.randomUUID().toString();
		StepVerifier.create(this.operations.opsForValue().set(id, "Hello World"))
			.expectNext(Boolean.TRUE)
			.expectComplete()
			.verify(Duration.ofSeconds(30));
		StepVerifier.create(this.operations.opsForValue().get(id))
			.expectNext("Hello World")
			.expectComplete()
			.verify(Duration.ofSeconds(30));
		StepVerifier.create(this.operations.execute((action) -> action.serverCommands().flushDb()))
			.expectNext("OK")
			.expectComplete()
			.verify(Duration.ofSeconds(30));
	}

}
