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

package org.springframework.boot.jdbc.autoconfigure;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import oracle.jdbc.OracleConnection;
import oracle.ucp.UniversalConnectionPoolException;
import oracle.ucp.admin.UniversalConnectionPoolManager;
import oracle.ucp.admin.UniversalConnectionPoolManagerImpl;
import oracle.ucp.jdbc.PoolDataSourceImpl;
import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.OracleUcpCheckpointRestoreLifecycle;
import org.springframework.boot.jdbc.autoconfigure.DataSourceCheckpointRestoreConfiguration.OracleUcp.OracleUcpCheckpointRestoreLifecycleRegistry;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.Lifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.util.Assert;

import static oracle.ucp.UniversalConnectionPoolLifeCycleState.LIFE_CYCLE_RUNNING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatList;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Tests for {@link OracleUcpCheckpointRestoreLifecycleRegistry} a {@link Lifecycle}
 * aggregating one {@link OracleUcpCheckpointRestoreLifecycle} per UCP data source found
 * in the context, which starts and stops those pools along with the context without
 * destroying them in between.
 * <p>
 * Two Spring facts shape the life cycle tests.
 * {@code OracleUcpCheckpointRestoreLifecycleRegistry} is a plain {@code Lifecycle} rather
 * than a {@code SmartLifecycle}, so the context does <em>not</em> auto start it on
 * refresh: a pool is only materialized once
 * {@link ConfigurableApplicationContext#start()} is called explicitly. On {@code close()}
 * the processor stops the life cycle beans first and destroys the singletons afterwards,
 * which is why a closed context leaves no pool behind at all.
 * <p>
 * The Universal Connection Pool Manager is a JVM-wide singleton, so this suite relies on
 * {@link #destroyAllConnectionPools()} and must be run sequentially, which is the JUnit
 * default.
 *
 * @author Fabio Grassi
 * @since 4.1.0
 */
class OracleUcpDataSourceCheckpointRestoreConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withPropertyValues("spring.datasource.generate-unique-name=true")
		.withConfiguration(AutoConfigurations.of(DataSourceCheckpointRestoreConfiguration.OracleUcp.class));

	@AfterEach
	void destroyAllConnectionPools() {
		poolNames().forEach(OracleUcpDataSourceCheckpointRestoreConfigurationTests::destroyPool);
	}

	@Test
	void poolDataSourceImplLifecycleIsAutoConfigured() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
			.run(context -> assertThat(context).hasSingleBean(OracleUcpCheckpointRestoreLifecycleRegistry.class));
	}

	@Test
	void poolDataSourceImplLifecycleIsNotAutoConfiguredWithoutUcpOnTheClasspath() {
		this.contextRunner.withUserConfiguration(OnePoolDataSourceConfiguration.class)
			.withClassLoader(new FilteredClassLoader(PoolDataSourceImpl.class))
			.run(context -> assertThat(context).doesNotHaveBean(OracleUcpCheckpointRestoreLifecycleRegistry.class));
	}

	@Test
	void poolDataSourceImplLifecycleIsNotAutoConfiguredWithoutOjdbcOnTheClasspath() {
		this.contextRunner.withUserConfiguration(OnePoolDataSourceConfiguration.class)
			.withClassLoader(new FilteredClassLoader(OracleConnection.class))
			.run(context -> assertThat(context).doesNotHaveBean(OracleUcpCheckpointRestoreLifecycleRegistry.class));
	}

	@Test
	void poolDataSourceImplLifecycleIsNotAutoConfiguredWithoutDataSource() {
		this.contextRunner.run(context -> {
			assertThat(context).doesNotHaveBean(DataSource.class);
			assertThat(context).doesNotHaveBean(OracleUcpCheckpointRestoreLifecycleRegistry.class);
		});
	}

	@Test
	void refreshingTheContextCreatesNoConnectionPool() {
		// A plain Lifecycle is not auto started, so merely refreshing the context must
		// leave the pools unmaterialized
		this.contextRunner.withUserConfiguration(TwoPoolDataSourcesConfiguration.class).run(context -> {
			assertThat(context).hasSingleBean(OracleUcpCheckpointRestoreLifecycleRegistry.class);
			assertThatList(poolNames()).as("Check that no pool has been created")
				.doesNotContain("firstPoolDataSource", "secondPoolDataSource");
		});
	}

	@Test
	void startingTheContextCreatesAndStartsTheConnectionPool() {
		this.contextRunner.withUserConfiguration(OnePoolDataSourceConfiguration.class).run(context -> {

			startContext(context);

			assertAll("Check that the pool has been created and started",
					() -> assertThatList(poolNames()).as("Check that the pool is registered")
						.contains("poolDataSource"),
					() -> assertThat(poolIsRunning("poolDataSource")).as("Check that the pool is running").isTrue(),
					() -> assertThat(lifecycleOf(context).isRunning()).as("Check isRunning").isTrue());
		});
	}

	@Test
	void startingTheContextStartsEveryConnectionPool() {
		this.contextRunner.withUserConfiguration(TwoPoolDataSourcesConfiguration.class).run(context -> {

			startContext(context);

			assertAll("Check that both pools are running",
					() -> assertThat(poolIsRunning("firstPoolDataSource")).as("Check first pool").isTrue(),
					() -> assertThat(poolIsRunning("secondPoolDataSource")).as("Check second pool").isTrue(),
					() -> assertThat(lifecycleOf(context).isRunning()).as("Check isRunning").isTrue());
		});
	}

	@Test
	void startingTheContextStartsTheConnectionPoolBehindADelegatingDataSource() {
		// Proves the unwrapping is wired end to end for the life cycle too
		this.contextRunner.withUserConfiguration(DelegatingDataSourceConfiguration.class).run(context -> {

			startContext(context);

			assertThat(poolIsRunning("delegatedPool")).as("Check that the pool is running").isTrue();
		});
	}

	@Test
	void startedConnectionPoolServesConnections() {
		// The point of starting the pools: the context is left with usable pools, not
		// merely registered ones
		this.contextRunner.withUserConfiguration(OnePoolDataSourceConfiguration.class).run(context -> {
			startContext(context);

			assertThatNoException().as("Check that a connection can be borrowed")
				.isThrownBy(() -> context.getBean("poolDataSource", DataSource.class).getConnection().close());
		});
	}

	@Test
	void stoppingTheContextStopsTheConnectionPoolWithoutDestroyingIt() {
		this.contextRunner.withUserConfiguration(OnePoolDataSourceConfiguration.class).run(context -> {
			startContext(context);

			stopContext(context);

			assertAll("Check that the pool has been stopped but is still registered",
					() -> assertThatList(poolNames()).as("Check that the pool still exists").contains("poolDataSource"),
					() -> assertThat(poolIsRunning("poolDataSource")).as("Check that the pool is not running")
						.isFalse(),
					() -> assertThat(lifecycleOf(context).isRunning()).as("Check isRunning").isFalse());
		});
	}

	@Test
	void restartingTheContextRestartsTheSameConnectionPool() {
		this.contextRunner.withUserConfiguration(OnePoolDataSourceConfiguration.class).run(context -> {
			startContext(context);
			stopContext(context);

			startContext(context);

			assertAll("Check that the very same pool has been restarted",
					() -> assertThatList(poolNames()).as("Check that no second pool exists")
						.containsOnlyOnce("poolDataSource"),
					() -> assertThat(poolIsRunning("poolDataSource")).as("Check that the pool is running again")
						.isTrue());
		});
	}

	@Test
	void startingTheContextTwiceIsHarmless() {
		// UCP fails a 'start' on an already running pool with UCP-45060, so the guards
		// in OracleUcpLifecycle are what make this safe
		this.contextRunner.withUserConfiguration(OnePoolDataSourceConfiguration.class).run(context -> {
			startContext(context);

			assertThatNoException().as("Check that a second start does not throw")
				.isThrownBy(() -> startContext(context));

			assertThat(poolIsRunning("poolDataSource")).as("Check that the pool is still running").isTrue();
		});
	}

	@Test
	void stoppingTheContextTwiceIsHarmless() {
		this.contextRunner.withUserConfiguration(OnePoolDataSourceConfiguration.class).run(context -> {
			startContext(context);
			stopContext(context);

			assertThatNoException().as("Check that a second stop does not throw")
				.isThrownBy(() -> stopContext(context));

			assertThat(poolIsRunning("poolDataSource")).as("Check that the pool is still not running").isFalse();
		});
	}

	@Test
	void nonUcpDataSourceIsIgnored() {
		this.contextRunner.withUserConfiguration(MixedDataSourcesConfiguration.class).run(context -> {

			startContext(context);

			assertAll("Check that only the UCP data source has a pool",
					() -> assertThat(poolIsRunning("poolDataSource")).as("Check the UCP pool").isTrue(),
					() -> assertThatList(poolNames())
						.as("Check that the plain JDBC data source has contributed no pool")
						.containsExactly("poolDataSource"));
		});
	}

	@Test
	void lifecycleIsContributedButEmptyWithoutAnyUcpDataSource() {
		// The bean is conditional on a DataSource, not on a UCP one, so a context with
		// only plain data sources still gets an aggregate with nothing to manage. An
		// empty aggregate reports itself as running, which merely means the context is
		// free to call stop() on it
		this.contextRunner.withUserConfiguration(OnlyJdbcDataSourceConfiguration.class).run(context -> {
			assertThat(context).hasSingleBean(OracleUcpCheckpointRestoreLifecycleRegistry.class);

			assertThatNoException().as("Check that starting and stopping are no-ops").isThrownBy(() -> {
				startContext(context);
				stopContext(context);
			});

			assertThatList(poolNames()).as("Check that no pool has been created").isEmpty();
		});
	}

	@Test
	void isRunningIsFalseUnlessEveryConnectionPoolIsRunning() {
		// The aggregate is conjunctive: one stopped pool is enough to report the whole
		// set as not running
		this.contextRunner.withUserConfiguration(TwoPoolDataSourcesConfiguration.class).run(context -> {
			startContext(context);
			assertThat(lifecycleOf(context).isRunning()).as("Check isRunning with both pools running").isTrue();

			stopPool("firstPoolDataSource");

			assertAll("Check that a single stopped pool flips the aggregate",
					() -> assertThat(lifecycleOf(context).isRunning()).as("Check isRunning").isFalse(),
					() -> assertThat(poolIsRunning("secondPoolDataSource")).as("Check that the other pool is untouched")
						.isTrue());
		});
	}

	@Test
	void startRecreatesAConnectionPoolDestroyedBehindItsBack() {
		// The destroyer unregisters a pool but leaves the data source's name set, so a
		// later context start must create it again instead of failing the lookup
		this.contextRunner.withUserConfiguration(OnePoolDataSourceConfiguration.class).run(context -> {
			startContext(context);
			destroyPool("poolDataSource");
			assertThatList(poolNames()).as("Check that the pool is gone").doesNotContain("poolDataSource");

			startContext(context);

			assertThat(poolIsRunning("poolDataSource"))
				.as("Check that the pool has been created again under the same name")
				.isTrue();
		});
	}

	private static void startContext(final AssertableApplicationContext context) {
		context.getSourceApplicationContext().start();
	}

	private static void stopContext(final AssertableApplicationContext context) {
		context.getSourceApplicationContext().stop();
	}

	private static Lifecycle lifecycleOf(final AssertableApplicationContext context) {
		return context.getBean(OracleUcpCheckpointRestoreLifecycleRegistry.class);
	}

	private static PoolDataSourceImpl createPoolDataSource(final String poolName) {
		final PoolDataSourceImpl poolDataSource = DataSourceBuilder.create()
			.url("jdbc:hsqldb:mem:test-" + UUID.randomUUID())
			.type(PoolDataSourceImpl.class)
			.build();
		try {
			poolDataSource.setConnectionPoolName(poolName);
		}
		catch (SQLException e) {
			throw new InvalidDataAccessApiUsageException(
					"Cannot set Oracle UCP connection pool name '" + poolName + "'", e);
		}
		return poolDataSource;
	}

	private static JDBCDataSource createJdbcDataSource() {
		return DataSourceBuilder.create()
			.url("jdbc:hsqldb:mem:test-" + UUID.randomUUID())
			.type(JDBCDataSource.class)
			.build();
	}

	@Configuration(proxyBeanMethods = false)
	static class OnePoolDataSourceConfiguration {

		@Bean
		DataSource poolDataSource() {
			return createPoolDataSource("poolDataSource");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TwoPoolDataSourcesConfiguration {

		@Bean
		DataSource firstPoolDataSource() {
			return createPoolDataSource("firstPoolDataSource");
		}

		@Bean
		DataSource secondPoolDataSource() {
			return createPoolDataSource("secondPoolDataSource");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class DelegatingDataSourceConfiguration {

		@Bean
		DataSource delegatingDataSource() {
			return new DelegatingDataSource(createPoolDataSource("delegatedPool"));
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MixedDataSourcesConfiguration {

		@Bean
		DataSource poolDataSource() {
			return createPoolDataSource("poolDataSource");
		}

		@Bean
		DataSource jdbcDataSource() {
			return createJdbcDataSource();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class OnlyJdbcDataSourceConfiguration {

		@Bean
		DataSource jdbcDataSource() {
			return createJdbcDataSource();
		}

	}

	private static List<String> poolNames() {
		try {
			return Arrays.asList(poolManager().getConnectionPoolNames());
		}
		catch (UniversalConnectionPoolException ucpe) {
			throw new IllegalStateException("Oracle connection pool listing failed", ucpe);
		}
	}

	private static boolean poolIsRunning(final String poolName) {
		Assert.hasText(poolName, "'poolName' must not be null");
		if (!poolNames().contains(poolName)) {
			return false;
		}
		try {
			return poolManager().getConnectionPool(poolName).getLifeCycleState() == LIFE_CYCLE_RUNNING;
		}
		catch (UniversalConnectionPoolException ucpe) {
			throw new IllegalStateException("Oracle connection pool lookup failed", ucpe);
		}
	}

	private static void destroyPool(final String poolName) {
		doWithManager(UniversalConnectionPoolManager::destroyConnectionPool, poolName);
	}

	private static void stopPool(final String poolName) {
		doWithManager(UniversalConnectionPoolManager::stopConnectionPool, poolName);
	}

	private static void doWithManager(final PoolCommand command, final String poolName) {
		Assert.hasText(poolName, "'poolName' must not be null");
		try {
			command.accept(poolManager(), poolName);
		}
		catch (UniversalConnectionPoolException ucpe) {
			throw new IllegalStateException("Oracle connection pool action failed", ucpe);
		}
	}

	private static UniversalConnectionPoolManager poolManager() throws UniversalConnectionPoolException {
		return UniversalConnectionPoolManagerImpl.getUniversalConnectionPoolManager();
	}

	@FunctionalInterface
	private interface PoolCommand {

		void accept(final UniversalConnectionPoolManager mgr, final String poolName)
				throws UniversalConnectionPoolException;

	}

}
