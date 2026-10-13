/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.redis;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.PemKeyStore;
import org.springframework.boot.testcontainers.service.connection.PemTrustStore;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.data.redis.core.RedisOperations;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests for Redis with SSL.
 *
 * @author Scott Frederick
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class SampleRedisApplicationSslTests {

	private static final Charset CHARSET = StandardCharsets.UTF_8;

	@Container
	@ServiceConnection
	@PemKeyStore(certificate = "classpath:ssl/test-client.crt", privateKey = "classpath:ssl/test-client.key")
	@PemTrustStore("classpath:ssl/test-ca.crt")
	static RedisContainer redis = TestImage.forContainer(SecureRedisContainer.class);

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
