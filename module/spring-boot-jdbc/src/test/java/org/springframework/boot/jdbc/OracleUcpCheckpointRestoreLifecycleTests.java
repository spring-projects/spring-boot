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

package org.springframework.boot.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import oracle.ucp.UniversalConnectionPoolException;
import oracle.ucp.admin.UniversalConnectionPoolManager;
import oracle.ucp.admin.UniversalConnectionPoolManagerImpl;
import oracle.ucp.jdbc.PoolDataSourceImpl;
import oracle.ucp.util.Strings;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import org.springframework.context.Lifecycle;
import org.springframework.util.Assert;

import static oracle.ucp.UniversalConnectionPoolLifeCycleState.LIFE_CYCLE_RUNNING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatList;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Tests for {@link OracleUcpCheckpointRestoreLifecycle}.
 * <p>
 * The class is a {@link Lifecycle} over the connection pool of a single
 * {@link PoolDataSourceImpl}, so its behaviour is pinned through the two things an
 * observer can see, namely the pool registered with the
 * {@link UniversalConnectionPoolManager} and that pool's life cycle state.
 * <p>
 * Two UCP facts drive most of these tests:
 * <ul>
 * <li>a pool data source has a {@code null} {@code connectionPoolName} until its pool is
 * created, at which point UCP generates one, so {@code start()} is also the operation
 * that materializes the pool;</li>
 * <li>UCP rejects a {@code start()} on a pool that is already running, and fails it with
 * {@code UCP-45060}, so the guards in {@code start()} and {@code stop()} are load bearing
 * rather than cosmetic.</li>
 * </ul>
 * <p>
 * The Universal Connection Pool Manager is a JVM-wide singleton, so this suite relies on
 * unique pool names and on {@link #destroyAllConnectionPools()}. It must be run
 * sequentially, which is the JUnit default.
 *
 * @author Fabio Grassi
 * @since 4.1.0
 */
class OracleUcpCheckpointRestoreLifecycleTests {

	@AfterEach
	void destroyAllConnectionPools() {
		poolNames().forEach(OracleUcpCheckpointRestoreLifecycleTests::poolDestroy);
	}

	@ParameterizedTest
	@NullAndEmptySource
	void constructorCreatesInstanceWithoutTouchingThePool(final @Nullable String poolName) {

		final PoolDataSourceImpl pds = createPoolDataSource(poolName);

		final OracleUcpCheckpointRestoreLifecycle lifecycle = new OracleUcpCheckpointRestoreLifecycle(pds);

		assertAll("Check that construction alone creates no pool",
				() -> assertThat(lifecycle).as("Check the instance").isNotNull(),
				() -> assertThat(pds.getConnectionPoolName()).as("Check that the pool has no name yet").isNull(),
				() -> assertThatList(poolNames()).as("Check that no pool has been registered").isEmpty());
	}

	@Test
	void constructorThrowsExceptionWhenPoolDataSourceIsNull() {
		assertThatThrownBy(() -> new OracleUcpCheckpointRestoreLifecycle(null))
			.isExactlyInstanceOf(IllegalArgumentException.class)
			.hasMessage("Non null PoolDataSourceImpl instance expected");
	}

	@Test
	void startCreatesAndStartsTheConnectionPool() {

		final String poolName = uniquePoolName("start");
		final OracleUcpCheckpointRestoreLifecycle lifecycle = new OracleUcpCheckpointRestoreLifecycle(
				createPoolDataSource(poolName));
		assertThatList(poolNames()).as("Check that no pool exists before starting").doesNotContain(poolName);

		lifecycle.start();

		assertAll("Check that the pool has been created and started",
				() -> assertThatList(poolNames()).as("Check that the pool is registered").contains(poolName),
				() -> assertThat(poolIsRunning(poolName)).as("Check that the pool is running").isTrue(),
				() -> assertThat(lifecycle.isRunning()).as("Check isRunning").isTrue());
	}

	@Test
	void startGeneratesAPoolNameWhenTheDataSourceHasNone() {

		// A pool data source has no name until its pool is created, at which point UCP
		// generates one
		final PoolDataSourceImpl pds = createPoolDataSource(null);
		final OracleUcpCheckpointRestoreLifecycle lifecycle = new OracleUcpCheckpointRestoreLifecycle(pds);

		lifecycle.start();

		final String poolName = pds.getConnectionPoolName();
		assertAll("Check that UCP has named and started the pool",
				() -> assertThat(poolName).as("Check the generated pool name").isNotNull(),
				() -> assertThatList(poolNames()).as("Check that the pool is registered").contains(poolName),
				() -> assertThat(lifecycle.isRunning()).as("Check isRunning").isTrue());
	}

	@Test
	void startAdoptsAnAlreadyCreatedConnectionPool() {

		// A pool created behind the lifecycle's back, for instance by the meter binder,
		// must be started rather than created a second time
		final String poolName = uniquePoolName("adopted");
		final PoolDataSourceImpl pds = createPoolDataSource(poolName);
		createPool(pds);
		assertThat(poolIsRunning(poolName)).as("Check that a freshly created pool is not running yet").isFalse();

		new OracleUcpCheckpointRestoreLifecycle(pds).start();

		assertAll("Check that the existing pool has been started in place",
				() -> assertThatList(poolNames()).as("Check that there is still a single pool")
					.containsExactly(poolName),
				() -> assertThat(poolIsRunning(poolName)).as("Check that the pool is running").isTrue());
	}

	@Test
	void startIsIdempotent() {

		// UCP fails a 'start' on a running pool with UCP-45060, so the guard in
		// 'start' is what makes a repeated call safe
		final String poolName = uniquePoolName("restart");
		final OracleUcpCheckpointRestoreLifecycle lifecycle = new OracleUcpCheckpointRestoreLifecycle(
				createPoolDataSource(poolName));
		lifecycle.start();

		assertThatNoException().as("Check that a second start does not throw").isThrownBy(lifecycle::start);

		assertAll("Check that the pool is untouched and still running",
				() -> assertThatList(poolNames()).as("Check that no second pool has been created")
					.containsExactly(poolName),
				() -> assertThat(poolIsRunning(poolName)).as("Check that the pool is running").isTrue());
	}

	@Test
	void stopStopsTheConnectionPoolWithoutDestroyingIt() {

		final String poolName = uniquePoolName("stop");
		final OracleUcpCheckpointRestoreLifecycle lifecycle = new OracleUcpCheckpointRestoreLifecycle(
				createPoolDataSource(poolName));
		lifecycle.start();

		lifecycle.stop();

		assertAll("Check that the pool has been stopped but is still registered",
				() -> assertThatList(poolNames()).as("Check that the pool still exists").contains(poolName),
				() -> assertThat(poolIsRunning(poolName)).as("Check that the pool is not running").isFalse(),
				() -> assertThat(lifecycle.isRunning()).as("Check isRunning").isFalse());
	}

	@Test
	void stopIsIdempotent() {

		final String poolName = uniquePoolName("doubleStop");
		final OracleUcpCheckpointRestoreLifecycle lifecycle = new OracleUcpCheckpointRestoreLifecycle(
				createPoolDataSource(poolName));
		lifecycle.start();
		lifecycle.stop();

		assertThatNoException().as("Check that a second stop does not throw").isThrownBy(lifecycle::stop);

		assertThat(poolIsRunning(poolName)).as("Check that the pool is still not running").isFalse();
	}

	@Test
	void stopDoesNothingWhenTheConnectionPoolWasNeverCreated() {

		final String poolName = uniquePoolName("neverCreated");
		final OracleUcpCheckpointRestoreLifecycle lifecycle = new OracleUcpCheckpointRestoreLifecycle(
				createPoolDataSource(poolName));

		assertThatNoException().as("Check that stopping an uncreated pool does not throw").isThrownBy(lifecycle::stop);

		assertThatList(poolNames()).as("Check that no pool has been created").isEmpty();
	}

	@Test
	void stopDoesNothingWhenTheConnectionPoolNameIsNull() {

		// Nothing can be looked up without a name, so 'stop' must be a no-op rather
		// than an error
		final PoolDataSourceImpl pds = createPoolDataSource(null);
		assertThat(pds.getConnectionPoolName()).as("Check that the pool has no name yet").isNull();

		assertThatNoException().as("Check that stopping an unnamed pool does not throw")
			.isThrownBy(new OracleUcpCheckpointRestoreLifecycle(pds)::stop);

		assertThatList(poolNames()).as("Check that no pool has been created").isEmpty();
	}

	@Test
	void startAfterStopRestartsTheConnectionPool() {

		final String poolName = uniquePoolName("cycle");
		final OracleUcpCheckpointRestoreLifecycle lifecycle = new OracleUcpCheckpointRestoreLifecycle(
				createPoolDataSource(poolName));
		lifecycle.start();
		lifecycle.stop();
		assertThat(lifecycle.isRunning()).as("Check that the pool is stopped").isFalse();

		lifecycle.start();

		assertAll("Check that the same pool has been restarted",
				() -> assertThatList(poolNames()).as("Check that no second pool has been created")
					.containsExactly(poolName),
				() -> assertThat(lifecycle.isRunning()).as("Check isRunning").isTrue());
	}

	@Test
	void startRecreatesAConnectionPoolDestroyedBehindItsBack() {

		// The destroyer unregisters the pool but leaves the data source's name set, so
		// a later 'start' must create the pool again instead of failing the lookup
		final String poolName = uniquePoolName("destroyed");
		final PoolDataSourceImpl pds = createPoolDataSource(poolName);
		final OracleUcpCheckpointRestoreLifecycle lifecycle = new OracleUcpCheckpointRestoreLifecycle(pds);
		lifecycle.start();
		poolDestroy(poolName);
		assertThatList(poolNames()).as("Check that the pool is gone").doesNotContain(poolName);

		lifecycle.start();

		assertAll("Check that the pool has been created again under the same name",
				() -> assertThatList(poolNames()).as("Check that the pool is registered again").contains(poolName),
				() -> assertThat(lifecycle.isRunning()).as("Check isRunning").isTrue());
	}

	@Test
	void isRunningReturnsFalseWhenTheConnectionPoolWasNeverCreated() {

		final OracleUcpCheckpointRestoreLifecycle lifecycle = new OracleUcpCheckpointRestoreLifecycle(
				createPoolDataSource(uniquePoolName("unstarted")));

		assertThat(lifecycle.isRunning()).as("Check that an uncreated pool is not running").isFalse();
	}

	@Test
	void isRunningReturnsFalseWhenTheConnectionPoolNameIsNull() {

		// No name means no lookup, which must be reported as 'not running' rather than
		// blowing up
		final PoolDataSourceImpl pds = createPoolDataSource(null);
		assertThat(pds.getConnectionPoolName()).as("Check that the pool has no name yet").isNull();

		assertThat(new OracleUcpCheckpointRestoreLifecycle(pds).isRunning()).as("Check isRunning").isFalse();
	}

	@Test
	void isRunningReturnsFalseForACreatedButNotYetStartedConnectionPool() {

		// UCP registers a freshly created pool in the 'Stopped' state
		final String poolName = uniquePoolName("created");
		final PoolDataSourceImpl pds = createPoolDataSource(poolName);
		createPool(pds);

		assertAll("Check that a created pool is not running until started",
				() -> assertThatList(poolNames()).as("Check that the pool is registered").contains(poolName),
				() -> assertThat(new OracleUcpCheckpointRestoreLifecycle(pds).isRunning()).as("Check isRunning")
					.isFalse());
	}

	@Test
	void isRunningReturnsFalseWhenTheConnectionPoolIsDestroyedBehindItsBack() {

		final String poolName = uniquePoolName("vanished");
		final OracleUcpCheckpointRestoreLifecycle lifecycle = new OracleUcpCheckpointRestoreLifecycle(
				createPoolDataSource(poolName));
		lifecycle.start();
		assertThat(lifecycle.isRunning()).as("Check that the pool is running first").isTrue();

		poolDestroy(poolName);

		assertThat(lifecycle.isRunning()).as("Check isRunning after the pool has vanished").isFalse();
	}

	@Test
	void startedConnectionPoolServesConnections() {

		// The point of starting the pool: proves the lifecycle leaves behind a usable
		// pool and not merely a registered one
		final String poolName = uniquePoolName("usable");
		final PoolDataSourceImpl pds = createPoolDataSource(poolName);
		new OracleUcpCheckpointRestoreLifecycle(pds).start();

		assertThatNoException().as("Check that a connection can be borrowed").isThrownBy(() -> {
			try (Connection connection = pds.getConnection()) {
				assertThat(connection.isValid(1)).as("Check that the connection is valid").isTrue();
			}
		});
	}

	@Test
	void lifecycleOnlyAffectsItsOwnConnectionPool() {

		final String ownPoolName = uniquePoolName("own");
		final String otherPoolName = uniquePoolName("other");
		final OracleUcpCheckpointRestoreLifecycle own = new OracleUcpCheckpointRestoreLifecycle(
				createPoolDataSource(ownPoolName));
		final OracleUcpCheckpointRestoreLifecycle other = new OracleUcpCheckpointRestoreLifecycle(
				createPoolDataSource(otherPoolName));
		own.start();
		other.start();

		own.stop();

		assertAll("Check that only the own pool has been stopped",
				() -> assertThat(own.isRunning()).as("Check the own pool").isFalse(),
				() -> assertThat(other.isRunning()).as("Check the other pool").isTrue(),
				() -> assertThatList(poolNames()).as("Check that both pools are still registered")
					.contains(ownPoolName, otherPoolName));
	}

	@Test
	void twoLifecyclesOverTheSameDataSourceShareTheConnectionPool() {

		// Nothing prevents two lifecycles from wrapping the same data source, and both
		// must then see the very same pool
		final String poolName = uniquePoolName("shared");
		final PoolDataSourceImpl pds = createPoolDataSource(poolName);
		final OracleUcpCheckpointRestoreLifecycle first = new OracleUcpCheckpointRestoreLifecycle(pds);
		final OracleUcpCheckpointRestoreLifecycle second = new OracleUcpCheckpointRestoreLifecycle(pds);

		first.start();

		assertAll("Check that the second lifecycle sees the pool started by the first one",
				() -> assertThatList(poolNames()).as("Check that a single pool has been created")
					.containsExactly(poolName),
				() -> assertThat(second.isRunning()).as("Check isRunning on the second lifecycle").isTrue(),
				() -> assertThatNoException().as("Check that the second start is a no-op").isThrownBy(second::start));

		second.stop();

		assertThat(first.isRunning()).as("Check that the first lifecycle sees the stop").isFalse();
	}

	private static String uniquePoolName(final String prefix) {
		return prefix + "-" + UUID.randomUUID();
	}

	private static PoolDataSourceImpl createPoolDataSource(final @Nullable String poolName) {
		final PoolDataSourceImpl poolDataSource = DataSourceBuilder.create()
			.url("jdbc:hsqldb:mem:test-" + UUID.randomUUID())
			.type(PoolDataSourceImpl.class)
			.build();
		if (!Strings.isNullOrEmpty(poolName)) {
			try {
				poolDataSource.setConnectionPoolName(poolName);
			}
			catch (SQLException e) {
				throw new IllegalStateException("Oracle connection pool initialization failed", e);
			}
		}
		return poolDataSource;
	}

	private static void createPool(final PoolDataSourceImpl poolDataSource) {
		try {
			poolDataSource.createUniversalConnectionPool();
		}
		catch (SQLException e) {
			throw new IllegalStateException("Oracle connection pool creation failed", e);
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

	private static void poolDestroy(final String poolName) {
		doWithManager(UniversalConnectionPoolManager::destroyConnectionPool, poolName);
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
