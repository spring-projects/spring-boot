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

package org.springframework.boot.docker.compose.service.connection.mongo;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for {@link MongoDockerComposeConnectionDetailsFactory}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
@Disabled
class XMongoDockerComposeConnectionDetailsFactoryTests {

	@Test
	void test() {
		fail("Not yet implemented");
	}

	// @formatter:off

	/*


	@Test
	void usernameUsesEnvVariable() {
		RunningService service = createService(Map.of("MONGO_INITDB_ROOT_USERNAME", "user-1"));
		MongoService mongoService = new MongoService(service);
		assertThat(mongoService.getUsername()).isEqualTo("user-1");
	}

	@Test
	void doesNotSupportUsernameFile() {
		RunningService service = createService(Map.of("MONGO_INITDB_ROOT_USERNAME_FILE", "/username.txt"));
		MongoService mongoService = new MongoService(service);
		assertThatThrownBy(mongoService::getUsername).isInstanceOf(IllegalStateException.class)
			.hasMessage("MONGO_INITDB_ROOT_USERNAME_FILE is not supported");
	}

	@Test
	void usernameHasDefault() {
		RunningService service = createService(Collections.emptyMap());
		MongoService mongoService = new MongoService(service);
		assertThat(mongoService.getUsername()).isNull();
	}

	@Test
	void passwordUsesEnvVariable() {
		RunningService service = createService(Map.of("MONGO_INITDB_ROOT_PASSWORD", "some-secret-1"));
		MongoService mongoService = new MongoService(service);
		assertThat(mongoService.getPassword()).isEqualTo("some-secret-1");
	}

	@Test
	void doesNotSupportPasswordFile() {
		RunningService service = createService(Map.of("MONGO_INITDB_ROOT_PASSWORD_FILE", "/username.txt"));
		MongoService mongoService = new MongoService(service);
		assertThatThrownBy(mongoService::getPassword).isInstanceOf(IllegalStateException.class)
			.hasMessage("MONGO_INITDB_ROOT_PASSWORD_FILE is not supported");
	}

	@Test
	void passwordHasDefault() {
		RunningService service = createService(Collections.emptyMap());
		MongoService mongoService = new MongoService(service);
		assertThat(mongoService.getPassword()).isNull();
	}

	@Test
	void databaseUsesEnvVariable() {
		RunningService service = createService(Map.of("MONGO_INITDB_DATABASE", "database-1"));
		MongoService mongoService = new MongoService(service);
		assertThat(mongoService.getDatabase()).isEqualTo("database-1");
	}

	@Test
	void databaseHasDefault() {
		RunningService service = createService(Collections.emptyMap());
		MongoService mongoService = new MongoService(service);
		assertThat(mongoService.getDatabase()).isNull();
	}

	@Test
	void getPort() {
		RunningService service = createService(Collections.emptyMap());
		MongoService mongoService = new MongoService(service);
		assertThat(mongoService.getPort()).isEqualTo(12345);
	}

	@Test
	void matches() {
		assertThat(MongoService.matches(createService(Collections.emptyMap()))).isTrue();
		assertThat(MongoService.matches(createService(ImageReference.parse("redis:7.1"), Collections.emptyMap())))
			.isFalse();
	}

	private RunningService createService(Map<String, String> env) {
		return createService(ImageReference.parse("mongo:6.0"), env);
	}

	private RunningService createService(ImageReference image, Map<String, String> env) {
		return RunningServiceBuilder.create("service-1", image).addTcpPort(27017, 12345).env(env).build();
	}



	 */

	// @formatter:on

}
