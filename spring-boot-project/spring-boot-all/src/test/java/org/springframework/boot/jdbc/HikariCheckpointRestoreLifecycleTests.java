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

package org.springframework.boot.jdbc;

import java.util.UUID;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HikariCheckpointRestoreLifecycle}.
 *
 * @author Christoph Strobl
 * @author Andy Wilkinson
 */
class HikariCheckpointRestoreLifecycleTests {

	private final HikariCheckpointRestoreLifecycle lifecycle;

	private final HikariDataSource dataSource;

	HikariCheckpointRestoreLifecycleTests() {
		HikariConfig config = new HikariConfig();
		config.setAllowPoolSuspension(true);
		config.setJdbcUrl("jdbc:hsqldb:mem:test-" + UUID.randomUUID());
		config.setPoolName("lifecycle-tests");
		this.dataSource = new HikariDataSource(config);
		this.lifecycle = new HikariCheckpointRestoreLifecycle(this.dataSource,
				mock(ConfigurableApplicationContext.class));
	}

	@Test
	void startedWhenStartedShouldSucceed() {
		assertThat(this.lifecycle.isRunning()).isTrue();
		this.lifecycle.start();
		assertThat(this.lifecycle.isRunning()).isTrue();
	}

	@Test
	void stopWhenStoppedShouldSucceed() {
		assertThat(this.lifecycle.isRunning()).isTrue();
		this.lifecycle.stop();
		assertThat(this.dataSource.isRunning()).isFalse();
		assertThatNoException().isThrownBy(this.lifecycle::stop);
	}

	@Test
	void whenStoppedAndStartedDataSourceShouldPauseAndResume() {
		assertThat(this.lifecycle.isRunning()).isTrue();
		this.lifecycle.stop();
		assertThat(this.dataSource.isRunning()).isFalse();
		assertThat(this.dataSource.isClosed()).isFalse();
		assertThat(this.lifecycle.isRunning()).isFalse();
		assertThat(this.dataSource.getHikariPoolMXBean().getTotalConnections()).isZero();
		this.lifecycle.start();
		assertThat(this.dataSource.isRunning()).isTrue();
		assertThat(this.dataSource.isClosed()).isFalse();
		assertThat(this.lifecycle.isRunning()).isTrue();
	}

	@Test
	void whenDataSourceIsClosedThenStartShouldThrow() {
		this.dataSource.close();
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(this.lifecycle::start);
	}

	@Test
	void startHasNoEffectWhenDataSourceIsNotAHikariDataSource() {
		HikariCheckpointRestoreLifecycle nonHikariLifecycle = new HikariCheckpointRestoreLifecycle(
				mock(DataSource.class), mock(ConfigurableApplicationContext.class));
		assertThat(nonHikariLifecycle.isRunning()).isFalse();
		nonHikariLifecycle.start();
		assertThat(nonHikariLifecycle.isRunning()).isFalse();
	}

	@Test
	void stopHasNoEffectWhenDataSourceIsNotAHikariDataSource() {
		HikariCheckpointRestoreLifecycle nonHikariLifecycle = new HikariCheckpointRestoreLifecycle(
				mock(DataSource.class), mock(ConfigurableApplicationContext.class));
		assertThat(nonHikariLifecycle.isRunning()).isFalse();
		nonHikariLifecycle.stop();
		assertThat(nonHikariLifecycle.isRunning()).isFalse();
	}

}
