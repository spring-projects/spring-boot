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

package org.springframework.boot.data.redis.autoconfigure;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.data.redis.domain.city.City;
import org.springframework.boot.data.redis.domain.city.CityRepository;
import org.springframework.boot.data.redis.domain.empty.EmptyPackage;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataRedisRepositoriesAutoConfiguration}.
 *
 * @author Eddú Meléndez
 */
@Testcontainers(disabledWithoutDocker = true)
class DataRedisRepositoriesAutoConfigurationTests {

	@Container
	public static RedisContainer redis = TestImage.container(RedisContainer.class);

	private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@BeforeEach
	void setUp() {
		TestPropertyValues
			.of("spring.data.redis.host=" + redis.getHost(), "spring.data.redis.port=" + redis.getFirstMappedPort())
			.applyTo(this.context.getEnvironment());
	}

	@AfterEach
	void close() {
		this.context.close();
	}

	@Test
	void testDefaultRepositoryConfiguration() {
		this.context.register(TestConfiguration.class, DataRedisAutoConfiguration.class,
				DataRedisRepositoriesAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(CityRepository.class)).isNotNull();
	}

	@Test
	void testNoRepositoryConfiguration() {
		this.context.register(EmptyConfiguration.class, DataRedisAutoConfiguration.class,
				DataRedisRepositoriesAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean("redisTemplate")).isNotNull();
	}

	@Test
	void doesNotTriggerDefaultRepositoryDetectionIfCustomized() {
		this.context.register(CustomizedConfiguration.class, DataRedisAutoConfiguration.class,
				DataRedisRepositoriesAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(CityRepository.class)).isNotNull();
	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(City.class)
	static class TestConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(EmptyPackage.class)
	static class EmptyConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(DataRedisRepositoriesAutoConfigurationTests.class)
	@EnableRedisRepositories(basePackageClasses = CityRepository.class)
	static class CustomizedConfiguration {

	}

}
