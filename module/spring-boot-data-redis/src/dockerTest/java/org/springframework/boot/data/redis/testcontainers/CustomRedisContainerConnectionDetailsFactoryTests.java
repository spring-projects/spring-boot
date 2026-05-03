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

package org.springframework.boot.data.redis.testcontainers;

import java.util.Map;
import java.util.function.Supplier;

import com.redis.testcontainers.RedisContainer;
import com.redis.testcontainers.RedisStackContainer;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetailsFactories;
import org.springframework.boot.data.redis.autoconfigure.DataRedisConnectionDetails;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.TestContainerConnectionSource;
import org.springframework.core.annotation.MergedAnnotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link RedisContainerConnectionDetailsFactory} when using a custom container
 * without "redis" as the name.
 *
 * @author Phillip Webb
 */
class CustomRedisContainerConnectionDetailsFactoryTests {

	@Test
	void getConnectionDetailsWhenRedisContainerWithCustomName() {
		ConnectionDetailsFactories factories = new ConnectionDetailsFactories(null);
		MergedAnnotation<ServiceConnection> annotation = MergedAnnotation.of(ServiceConnection.class,
				Map.of("value", ""));
		Supplier<RedisContainer> containerSupplier = () -> new RedisContainer("redis");
		ContainerConnectionSource<RedisContainer> source = TestContainerConnectionSource.create("test",
				mock(Origin.class), RedisContainer.class, "mycustomimage", annotation, containerSupplier);
		Map<Class<?>, ConnectionDetails> connectionDetails = factories.getConnectionDetails(source, true);
		assertThat(connectionDetails.get(DataRedisConnectionDetails.class)).isNotNull();
	}

	@Test
	void getConnectionDetailsWhenRedisStackContainerWithCustomName() {
		ConnectionDetailsFactories factories = new ConnectionDetailsFactories(null);
		MergedAnnotation<ServiceConnection> annotation = MergedAnnotation.of(ServiceConnection.class,
				Map.of("value", ""));
		Supplier<RedisStackContainer> containerSupplier = () -> new RedisStackContainer("redis");
		ContainerConnectionSource<RedisStackContainer> source = TestContainerConnectionSource.create("test",
				mock(Origin.class), RedisStackContainer.class, "mycustomimage", annotation, containerSupplier);
		Map<Class<?>, ConnectionDetails> connectionDetails = factories.getConnectionDetails(source, true);
		assertThat(connectionDetails.get(DataRedisConnectionDetails.class)).isNotNull();
	}

}
