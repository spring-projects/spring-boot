/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.cassandra;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link CassandraAutoConfiguration} that only uses password authentication.
 *
 * @author Stephane Nicoll
 */
@Testcontainers(disabledWithoutDocker = true)
class CassandraAutoConfigurationWithPasswordAuthenticationIntegrationTests {

	@Container
	static final CassandraContainer<?> cassandra = new PasswordAuthenticatorCassandraContainer().withStartupAttempts(5)
			.withStartupTimeout(Duration.ofMinutes(10));

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(CassandraAutoConfiguration.class)).withPropertyValues(
					"spring.data.cassandra.contact-points:" + cassandra.getHost() + ":"
							+ cassandra.getFirstMappedPort(),
					"spring.data.cassandra.local-datacenter=datacenter1", "spring.data.cassandra.read-timeout=20s",
					"spring.data.cassandra.connect-timeout=10s");

	@Test
	void authenticationWithValidUsernameAndPassword() {
		this.contextRunner.withPropertyValues("spring.data.cassandra.username=cassandra",
				"spring.data.cassandra.password=cassandra").run((context) -> {
					SimpleStatement select = SimpleStatement.newInstance("SELECT release_version FROM system.local")
							.setConsistencyLevel(ConsistencyLevel.LOCAL_ONE);
					assertThat(context.getBean(CqlSession.class).execute(select).one()).isNotNull();
				});
	}

	@Test
	void authenticationWithInvalidCredentials() {
		this.contextRunner
				.withPropertyValues("spring.data.cassandra.username=not-a-user",
						"spring.data.cassandra.password=invalid-password")
				.run((context) -> assertThatThrownBy(() -> context.getBean(CqlSession.class))
						.hasMessageContaining("Authentication error"));
	}

	static final class PasswordAuthenticatorCassandraContainer
			extends CassandraContainer<PasswordAuthenticatorCassandraContainer> {

		@Override
		protected void containerIsCreated(String containerId) {
			String config = this.copyFileFromContainer("/etc/cassandra/cassandra.yaml",
					(stream) -> StreamUtils.copyToString(stream, StandardCharsets.UTF_8));
			String updatedConfig = config.replace("authenticator: AllowAllAuthenticator",
					"authenticator: PasswordAuthenticator");
			this.copyFileToContainer(Transferable.of(updatedConfig.getBytes(StandardCharsets.UTF_8)),
					"/etc/cassandra/cassandra.yaml");
		}

	}

}
