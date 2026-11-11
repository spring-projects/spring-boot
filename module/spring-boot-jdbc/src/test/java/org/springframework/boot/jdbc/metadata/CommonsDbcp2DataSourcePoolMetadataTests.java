/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.metadata;

import java.time.Duration;

import org.apache.commons.dbcp2.BasicDataSource;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CommonsDbcp2DataSourcePoolMetadata}.
 *
 * @author Stephane Nicoll
 */
class CommonsDbcp2DataSourcePoolMetadataTests
		extends AbstractDataSourcePoolMetadataTests<CommonsDbcp2DataSourcePoolMetadata> {

	private final CommonsDbcp2DataSourcePoolMetadata dataSourceMetadata = createDataSourceMetadata(0, 2);

	@Override
	protected CommonsDbcp2DataSourcePoolMetadata getDataSourceMetadata() {
		return this.dataSourceMetadata;
	}

	@Test
	void getPoolUsageWithNoCurrent() {
		CommonsDbcp2DataSourcePoolMetadata dsm = new CommonsDbcp2DataSourcePoolMetadata(createDataSource()) {
			@Override
			public @Nullable Integer getActive() {
				return null;
			}
		};
		assertThat(dsm.getUsage()).isNull();
	}

	@Test
	void getPoolUsageWithNoMax() {
		CommonsDbcp2DataSourcePoolMetadata dsm = new CommonsDbcp2DataSourcePoolMetadata(createDataSource()) {
			@Override
			public @Nullable Integer getMax() {
				return null;
			}
		};
		assertThat(dsm.getUsage()).isNull();
	}

	@Test
	void getPoolUsageWithUnlimitedPool() {
		DataSourcePoolMetadata unlimitedDataSource = createDataSourceMetadata(0, -1);
		assertThat(unlimitedDataSource.getUsage()).isEqualTo(-1f);
	}

	@Override
	public void getValidationQuery() {
		BasicDataSource dataSource = createDataSource();
		dataSource.setValidationQuery("SELECT FROM FOO");
		assertThat(new CommonsDbcp2DataSourcePoolMetadata(dataSource).getValidationQuery())
			.isEqualTo("SELECT FROM FOO");
	}

	@Override
	public void getDefaultAutoCommit() {
		BasicDataSource dataSource = createDataSource();
		dataSource.setDefaultAutoCommit(false);
		assertThat(new CommonsDbcp2DataSourcePoolMetadata(dataSource).getDefaultAutoCommit()).isFalse();
	}

	private CommonsDbcp2DataSourcePoolMetadata createDataSourceMetadata(int minSize, int maxSize) {
		BasicDataSource dataSource = createDataSource();
		dataSource.setMinIdle(minSize);
		dataSource.setMaxTotal(maxSize);
		dataSource.setMinEvictableIdle(Duration.ofSeconds(5));
		return new CommonsDbcp2DataSourcePoolMetadata(dataSource);
	}

	private BasicDataSource createDataSource() {
		return initializeBuilder().type(BasicDataSource.class).build();
	}

}
