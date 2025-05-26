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

package org.springframework.boot.testcontainers.service.connection.hazelcast;

import java.util.UUID;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.hazelcast.HazelcastAutoConfiguration;
import org.springframework.boot.autoconfigure.hazelcast.HazelcastConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.HazelcastContainer;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HazelcastContainerConnectionDetailsFactory}.
 *
 * @author Dmytro Nosan
 */
@SpringJUnitConfig
@Testcontainers(disabledWithoutDocker = true)
class HazelcastContainerConnectionDetailsFactoryIntegrationTests {

	@Container
	@ServiceConnection
	static final HazelcastContainer hazelcast = TestImage.container(HazelcastContainer.class);

	@Autowired(required = false)
	private HazelcastConnectionDetails connectionDetails;

	@Autowired
	private HazelcastInstance hazelcastInstance;

	@Test
	void connectionCanBeMadeToHazelcastContainer() {
		assertThat(this.connectionDetails).isNotNull();
		IMap<String, String> map = this.hazelcastInstance.getMap(UUID.randomUUID().toString());
		map.put("test", "containers");
		assertThat(map.get("test")).isEqualTo("containers");
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration(HazelcastAutoConfiguration.class)
	static class TestConfiguration {

	}

}
