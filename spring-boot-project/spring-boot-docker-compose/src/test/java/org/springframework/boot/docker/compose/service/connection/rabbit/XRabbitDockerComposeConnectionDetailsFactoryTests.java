/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.docker.compose.service.connection.rabbit;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for {@link RabbitDockerComposeConnectionDetailsFactory}
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
@Disabled
class XRabbitDockerComposeConnectionDetailsFactoryTests {

	@Test
	void test() {
		fail("Not yet implemented");
	}

	// @formatter:off

	/*


	@Test
	void usernameUsesEnvVariable() {
		RunningService service = createService(Map.of("RABBITMQ_DEFAULT_USER", "user-1"));
		RabbitService rabbitService = new RabbitService(service);
		assertThat(rabbitService.getUsername()).isEqualTo("user-1");
	}

	@Test
	void usernameHasDefault() {
		RunningService service = createService(Collections.emptyMap());
		RabbitService rabbitService = new RabbitService(service);
		assertThat(rabbitService.getUsername()).isEqualTo("guest");
	}

	@Test
	void passwordUsesEnvVariable() {
		RunningService service = createService(Map.of("RABBITMQ_DEFAULT_PASS", "secret-1"));
		RabbitService rabbitService = new RabbitService(service);
		assertThat(rabbitService.getPassword()).isEqualTo("secret-1");
	}

	@Test
	void passwordHasDefault() {
		RunningService service = createService(Collections.emptyMap());
		RabbitService rabbitService = new RabbitService(service);
		assertThat(rabbitService.getPassword()).isEqualTo("guest");
	}

	@Test
	void getPort() {
		RunningService service = createService(Collections.emptyMap());
		RabbitService rabbitService = new RabbitService(service);
		assertThat(rabbitService.getPort()).isEqualTo(15672);
	}

	@Test
	void matches() {
		assertThat(RabbitService.matches(createService(Collections.emptyMap()))).isTrue();
		assertThat(RabbitService.matches(createService(ImageReference.parse("redis:7.1"), Collections.emptyMap())))
			.isFalse();
	}

	private RunningService createService(Map<String, String> env) {
		return createService(ImageReference.parse("rabbitmq:3.11"), env);
	}

	private RunningService createService(ImageReference image, Map<String, String> env) {
		return RunningServiceBuilder.create("service-1", image).addTcpPort(5672, 15672).env(env).build();
	}


	 */

	// @formatter:on

}
