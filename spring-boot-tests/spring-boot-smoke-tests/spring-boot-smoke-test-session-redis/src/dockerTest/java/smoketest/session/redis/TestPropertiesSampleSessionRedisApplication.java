/*
 * Copyright 2012-2024 the original author or authors.
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

package smoketest.session.redis;

import com.redis.testcontainers.RedisContainer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;

public class TestPropertiesSampleSessionRedisApplication {

	public static void main(String[] args) {
		SpringApplication.from(SampleSessionRedisApplication::main).with(ContainerConfiguration.class).run(args);
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class ContainerConfiguration {

		@Bean
		RedisContainer redisContainer(DynamicPropertyRegistry properties) {
			RedisContainer container = TestImage.container(RedisContainer.class);
			properties.add("spring.data.redis.host", container::getHost);
			properties.add("spring.data.redis.port", container::getFirstMappedPort);
			return container;
		}

	}

}
