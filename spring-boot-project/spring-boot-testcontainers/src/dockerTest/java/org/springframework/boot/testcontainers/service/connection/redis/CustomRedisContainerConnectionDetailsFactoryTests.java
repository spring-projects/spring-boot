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

package org.springframework.boot.testcontainers.service.connection.redis;

import java.util.Map;

import com.redis.testcontainers.RedisContainer;
import com.redis.testcontainers.RedisStackContainer;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetailsFactories;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.TestContainerConnectionSource;
import org.springframework.core.annotation.MergedAnnotation;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link RedisContainerConnectionDetailsFactory} when using a custom container
 * without "redis" as the name.
 *
 * @author Phillip Webb
 */
class CustomRedisContainerConnectionDetailsFactoryTests {

	@Test
	void getConnectionDetailsWhenRedisContainerWithCustomName() {
		ConnectionDetailsFactories factories = new ConnectionDetailsFactories();
		MergedAnnotation<ServiceConnection> annotation = MergedAnnotation.of(ServiceConnection.class,
				Map.of("value", ""));
		ContainerConnectionSource<RedisContainer> source = TestContainerConnectionSource.create("test", null,
				RedisContainer.class, "mycustomimage", annotation, null);
		Map<Class<?>, ConnectionDetails> connectionDetails = factories.getConnectionDetails(source, true);
		assertThat(connectionDetails.get(RedisConnectionDetails.class)).isNotNull();
	}

	@Test
	void getConnectionDetailsWhenRedisStackContainerWithCustomName() {
		ConnectionDetailsFactories factories = new ConnectionDetailsFactories();
		MergedAnnotation<ServiceConnection> annotation = MergedAnnotation.of(ServiceConnection.class,
				Map.of("value", ""));
		ContainerConnectionSource<RedisStackContainer> source = TestContainerConnectionSource.create("test", null,
				RedisStackContainer.class, "mycustomimage", annotation, null);
		Map<Class<?>, ConnectionDetails> connectionDetails = factories.getConnectionDetails(source, true);
		assertThat(connectionDetails.get(RedisConnectionDetails.class)).isNotNull();
	}

}
