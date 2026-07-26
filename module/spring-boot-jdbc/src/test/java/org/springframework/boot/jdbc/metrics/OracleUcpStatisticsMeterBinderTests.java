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

package org.springframework.boot.jdbc.metrics;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;
import java.util.function.DoubleSupplier;

import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import oracle.ucp.UniversalConnectionPoolException;
import oracle.ucp.admin.UniversalConnectionPoolManager;
import oracle.ucp.admin.UniversalConnectionPoolManagerImpl;
import oracle.ucp.jdbc.PoolDataSourceImpl;
import oracle.ucp.util.Strings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.metrics.OracleUcpStatisticsMeterBinder.MeterInfo;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.util.Assert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCollection;
import static org.assertj.core.api.Assertions.assertThatList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.boot.jdbc.metrics.OracleUcpStatisticsMeterBinder.METRIC_PREFIX;

/**
 * Tests for {@link oracle.ucp.jdbc.PoolDataSourceImpl}.
 *
 * @author Fabio Grassi
 * @since 4.1.0
 */
class OracleUcpStatisticsMeterBinderTests {

	private static final String POOL_NAME = "testPool";

	static final MeterInfo[] METER_INFOS = OracleUcpStatisticsMeterBinder.METER_INFOS;

	@AfterEach
	void destroyAllOracleConnectionPools() {
		try {
			final UniversalConnectionPoolManager mgr = UniversalConnectionPoolManagerImpl
				.getUniversalConnectionPoolManager();
			for (String poolName : mgr.getConnectionPoolNames()) {
				mgr.destroyConnectionPool(poolName);
			}
		}
		catch (UniversalConnectionPoolException e) {
			throw new InvalidDataAccessApiUsageException("Error while destroying Oracle connection pools", e);
		}
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { POOL_NAME })
	void constructorCreatesInstance(final String poolName) {
		final PoolDataSourceImpl pds = createPoolDataSource(poolName);
		try (final OracleUcpStatisticsMeterBinder binder = new OracleUcpStatisticsMeterBinder(pds)) {
			assertThat(binder).isNotNull();
		}
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { POOL_NAME })
	void staticFactoryMethodCreatesInstance(final String poolName) {
		final PoolDataSourceImpl pds = createPoolDataSource(poolName);
		try (final OracleUcpStatisticsMeterBinder binder = OracleUcpStatisticsMeterBinder.of(pds)) {
			assertThat(binder).isNotNull();
		}
	}

	@Test
	void constructorThrowsExceptionWhenPoolDataSourceIsNull() {
		assertThatThrownBy(() -> new OracleUcpStatisticsMeterBinder(null))
			.isExactlyInstanceOf(NullPointerException.class)
			.hasMessage("'poolDataSource' must not be null");
	}

	@Test
	void staticFactoryMethodThrowsExceptionWhenPoolDataSourceIsNull() {
		assertThatThrownBy(() -> OracleUcpStatisticsMeterBinder.of(null))
			.isExactlyInstanceOf(NullPointerException.class)
			.hasMessage("'poolDataSource' must not be null");
	}

	@Test
	void bindToRegistersExpectedNumberOfMetersAndCloseRemovesAllMeters() {
		final MeterRegistry registry = new SimpleMeterRegistry();
		try {
			final PoolDataSourceImpl pds = createPoolDataSource(POOL_NAME);
			try (final OracleUcpStatisticsMeterBinder binder = OracleUcpStatisticsMeterBinder.of(pds)) {
				assertThatList(registry.getMeters()).isEmpty();
				binder.bindTo(registry);
				assertThatList(registry.getMeters()).hasSize(METER_INFOS.length);
			}
			assertThatList(registry.getMeters()).isEmpty();
		}
		finally {
			registry.close();
		}
	}

	@ParameterizedTest
	@FieldSource("METER_INFOS")
	void bindToRegistersMetersWithExpectedProperties(final MeterInfo meterInfo) {

		final String name = METRIC_PREFIX + meterInfo.name();
		final Tags tags = Tags.of("pool", POOL_NAME).and(meterInfo.specificTags());

		final MeterRegistry registry = new SimpleMeterRegistry();
		try {
			final PoolDataSourceImpl pds = createPoolDataSource(POOL_NAME);
			try (final OracleUcpStatisticsMeterBinder binder = OracleUcpStatisticsMeterBinder.of(pds)) {

				binder.bindTo(registry);

				// Find all registered meters having a the given name and tags
				final Collection<Meter> meters = registry.find(name).tags(tags).meters();

				// Check that there is one and only one meter with the given key
				assertThat(meters).isNotNull();
				assertThatCollection(meters).hasSize(1);
				final Meter meter = meters.iterator().next();
				assertThat(meter).isNotNull();
				final Meter.Id meterId = meter.getId();

				// For the single meter found, check all other relevant properties
				assertAll("Check all relevant properties of meter " + meterId,
						() -> assertThat(meterId.getName()).as("Check name").isEqualTo(name),
						() -> assertThat(Tags.of(meterId.getTags())).as("Check tags").isEqualTo(tags),
						() -> assertThat(meterId.getDescription()).as("Check description")
							.isEqualTo(meterInfo.description()),
						() -> assertThat(meterId.getBaseUnit()).as("Check baseUnit").isEqualTo(meterInfo.baseUnit()),
						() -> assertThat(meterId.getType()).as("Check type").isEqualTo(meterInfo.type()));
			}
		}
		finally {
			registry.close();
		}
	}

	@ParameterizedTest
	@FieldSource("METER_INFOS")
	void registeredMetersReturnSameValuesAsStateObject(final MeterInfo meterInfo) throws SQLException {

		final String name = METRIC_PREFIX + meterInfo.name();
		final Tags tags = Tags.of("pool", POOL_NAME).and(meterInfo.specificTags());

		final MeterRegistry registry = new SimpleMeterRegistry();
		try {
			final int initSize = 1;
			final int minSize = 2;
			final int maxSize = 4;
			final PoolDataSourceImpl pds = createPoolDataSource(POOL_NAME, initSize, minSize, maxSize);
			try (final OracleUcpStatisticsMeterBinder binder = OracleUcpStatisticsMeterBinder.of(pds)) {

				binder.bindTo(registry);

				final Collection<Meter> meters = registry.find(name).tags(tags).meters();
				final Meter meter = meters.iterator().next();
				final Iterator<Measurement> mesures = meter.measure().iterator();
				final Measurement measure = mesures.next();

				final DoubleSupplier expected = () -> meterInfo.function().applyAsDouble(pds.getStatistics());
				final DoubleSupplier actual = measure::getValue;

				assertThat(actual.getAsDouble()).as("When pool created but not yet running")
					.isEqualTo(expected.getAsDouble());

				startPool(pds.getConnectionPoolName());

				assertThat(actual.getAsDouble()).as("When pool running, but no connection borrowed")
					.isEqualTo(expected.getAsDouble());

				final Connection conn1 = pds.getConnection();

				assertThat(actual.getAsDouble()).as("When number of borrowed connections is equal to 'intialPoolSize'")
					.isEqualTo(expected.getAsDouble());

				final Connection conn2 = pds.getConnection();

				assertThat(actual.getAsDouble()).as("When number of borrowed connections is equal to 'minPoolSize'")
					.isEqualTo(expected.getAsDouble());

				final Connection conn3 = pds.getConnection();

				assertThat(actual.getAsDouble())
					.as("When number of borrowed connections is between 'minPoolSize' and 'maxPoolSize'")
					.isEqualTo(expected.getAsDouble());

				final Connection conn4 = pds.getConnection();

				assertThat(actual.getAsDouble()).as("When number of borrowed connections is equal to 'maxPoolSize'")
					.isEqualTo(expected.getAsDouble());

				await()
					.untilAsserted(() -> assertThatThrownBy(pds::getConnection).isExactlyInstanceOf(SQLException.class)
						.hasFieldOrPropertyWithValue("errorCode", 29)
						.hasMessageStartingWith("UCP-29"));

				assertThat(actual.getAsDouble()).as("After a timeout waiting for a connection while pool is exausted")
					.isEqualTo(expected.getAsDouble());

				conn1.close();
				conn2.close();

				assertThat(actual.getAsDouble())
					.as("When number of borrowed connections has returned between 'minPoolSize' and 'maxPoolSize'")
					.isEqualTo(expected.getAsDouble());

				conn3.close();
				conn4.close();

				assertThat(actual.getAsDouble())
					.as("When number of borrowed connections has returned below 'minPoolSize'")
					.isEqualTo(expected.getAsDouble());

				purgePool(pds.getConnectionPoolName());

				assertThat(actual.getAsDouble()).as("When pool has just been purged").isEqualTo(expected.getAsDouble());

				stopPool(pds.getConnectionPoolName());
				startPool(pds.getConnectionPoolName());

				assertThat(actual.getAsDouble()).as("When pool has just been restarted")
					.isEqualTo(expected.getAsDouble());

				destroyPool(pds.getConnectionPoolName());

				assertThat(actual.getAsDouble()).as("When pool has just been destroyed")
					.isEqualTo(expected.getAsDouble());

			}
		}
		finally {
			registry.close();
		}
	}

	private PoolDataSourceImpl createPoolDataSource(final String poolName) {
		return createPoolDataSource(poolName, 0, 0, 0);
	}

	private PoolDataSourceImpl createPoolDataSource(final String poolName, final int InitialSize, final int minSize,
			final int MaxSize) {
		final PoolDataSourceImpl poolDataSource = DataSourceBuilder.create()
			.url("jdbc:hsqldb:mem:test-" + UUID.randomUUID())
			.type(PoolDataSourceImpl.class)
			.build();
		try {
			if (!Strings.isNullOrEmpty(poolName)) {
				poolDataSource.setConnectionPoolName(poolName);
			}
			poolDataSource.setInitialPoolSize(InitialSize);
			poolDataSource.setMinPoolSize(minSize);
			poolDataSource.setMaxPoolSize(MaxSize);
			poolDataSource.setConnectionWaitDurationInMillis(100);
		}
		catch (SQLException e) {
			throw new IllegalStateException("Oracle connection pool initialization failed", e);
		}
		return poolDataSource;
	}

	private static final void destroyPool(final String poolName) {
		doWithManager(UniversalConnectionPoolManager::destroyConnectionPool, poolName);
	}

	private static final void purgePool(final String poolName) {
		doWithManager(UniversalConnectionPoolManager::purgeConnectionPool, poolName);
	}

	private static final void startPool(final String poolName) {
		doWithManager(UniversalConnectionPoolManager::startConnectionPool, poolName);
	}

	private static final void stopPool(final String poolName) {
		doWithManager(UniversalConnectionPoolManager::stopConnectionPool, poolName);
	}

	private static void doWithManager(final ConnectionPoolAction action, final String poolName) {
		Assert.hasText(poolName, "'poolName' must not be null");
		try {
			final UniversalConnectionPoolManager mgr = UniversalConnectionPoolManagerImpl
				.getUniversalConnectionPoolManager();
			action.accept(mgr, poolName);
		}
		catch (UniversalConnectionPoolException e) {
			throw new IllegalStateException("Oracle connection pool action failed", e);
		}
	}

	@FunctionalInterface
	private interface ConnectionPoolAction {

		void accept(final UniversalConnectionPoolManager mgr, final String poolName)
				throws UniversalConnectionPoolException;

	}

}
