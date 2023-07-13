/*
 * Copyright 2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.jdbc;

import java.util.UUID;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link HikariLifecycle}.
 *
 * @author Christoph Strobl
 */
class HikariLifecycleTests {

	@Test
	void stopStartHikariDataSource() {

		HikariLifecycle hikariLifecycle = createLifecycle();

		assertThat(hikariLifecycle.isRunning()).isTrue();

		hikariLifecycle.stop();

		assertThat(hikariLifecycle.getManagedInstance().isRunning()).isFalse();
		assertThat(hikariLifecycle.getManagedInstance().isClosed()).isFalse();
		assertThat(hikariLifecycle.isRunning()).isFalse();
		assertThat(hikariLifecycle.getManagedInstance().getHikariPoolMXBean().getTotalConnections()).isZero();

		hikariLifecycle.start();

		assertThat(hikariLifecycle.getManagedInstance().isRunning()).isTrue();
		assertThat(hikariLifecycle.getManagedInstance().isClosed()).isFalse();
		assertThat(hikariLifecycle.isRunning()).isTrue();
	}

	@Test
	void cannotStartClosedDataSource() {

		HikariLifecycle hikariLifecycle = createLifecycle();
		hikariLifecycle.getManagedInstance().close();

		assertThatExceptionOfType(RuntimeException.class).isThrownBy(hikariLifecycle::start);
	}

	HikariLifecycle createLifecycle() {

		HikariConfig config = new HikariConfig();
		config.setAllowPoolSuspension(true);
		config.setJdbcUrl("jdbc:hsqldb:mem:test-" + UUID.randomUUID());
		config.setPoolName("lifecycle-tests");

		HikariDataSource source = new HikariDataSource(config);
		return new HikariLifecycle(source);
	}

}
