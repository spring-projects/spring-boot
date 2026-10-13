/*
 * Copyright 2012-present the original author or authors.
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
