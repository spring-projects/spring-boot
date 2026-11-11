/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.redis.test.autoconfigure;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the {@link DataRedisTest#properties properties} attribute of
 * {@link DataRedisTest @DataRedisTest}.
 *
 * @author Artsiom Yudovin
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
@Testcontainers(disabledWithoutDocker = true)
@DataRedisTest(properties = "spring.profiles.active=test")
class DataRedisTestPropertiesIntegrationTests {

	@Container
	@ServiceConnection
	static final RedisContainer redis = TestImage.forContainer(RedisContainer.class);

	@Autowired
	private Environment environment;

	@Test
	void environmentWithNewProfile() {
		assertThat(this.environment.getActiveProfiles()).containsExactly("test");
	}

	@Nested
	class NestedTests {

		@Autowired
		private Environment innerEnvironment;

		@Test
		void propertiesFromEnclosingClassAffectNestedTests() {
			assertThat(DataRedisTestPropertiesIntegrationTests.this.environment.getActiveProfiles())
				.containsExactly("test");
			assertThat(this.innerEnvironment.getActiveProfiles()).containsExactly("test");
		}

	}

}
