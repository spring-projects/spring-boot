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

package smoketest.data.redis;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest;
import org.springframework.boot.testcontainers.service.connection.PemKeyStore;
import org.springframework.boot.testcontainers.service.connection.PemTrustStore;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.data.redis.core.RedisOperations;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests for Redis using Jedis with SSL.
 *
 * @author Scott Frederick
 */
@Testcontainers(disabledWithoutDocker = true)
@ClassPathExclusions("lettuce-core-*.jar")
@DataRedisTest
class SampleRedisApplicationJedisSslTests {

	private static final Charset CHARSET = StandardCharsets.UTF_8;

	@Container
	@ServiceConnection
	@PemKeyStore(certificate = "classpath:ssl/test-client.crt", privateKey = "classpath:ssl/test-client.key")
	@PemTrustStore("classpath:ssl/test-ca.crt")
	static RedisContainer redis = TestImage.container(SecureRedisContainer.class);

	@Autowired
	private RedisOperations<Object, Object> operations;

	@Autowired
	private SampleRepository exampleRepository;

	@Test
	void testRepository() {
		PersonHash personHash = new PersonHash();
		personHash.setDescription("Look, new @DataRedisTest!");
		assertThat(personHash.getId()).isNull();
		PersonHash savedEntity = this.exampleRepository.save(personHash);
		assertThat(savedEntity.getId()).isNotNull();
		assertThat(this.operations
			.execute((org.springframework.data.redis.connection.RedisConnection connection) -> connection.keyCommands()
				.exists(("persons:" + savedEntity.getId()).getBytes(CHARSET))))
			.isTrue();
		this.exampleRepository.deleteAll();
	}

}
