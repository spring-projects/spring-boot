/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.metadata;

import org.apache.tomcat.jdbc.pool.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TomcatDataSourcePoolMetadata}.
 *
 * @author Stephane Nicoll
 */
public class TomcatDataSourcePoolMetadataTests
		extends AbstractDataSourcePoolMetadataTests<TomcatDataSourcePoolMetadata> {

	private final TomcatDataSourcePoolMetadata dataSourceMetadata = new TomcatDataSourcePoolMetadata(
			createDataSource(0, 2));

	@Override
	protected TomcatDataSourcePoolMetadata getDataSourceMetadata() {
		return this.dataSourceMetadata;
	}

	@Override
	public void getValidationQuery() {
		DataSource dataSource = createDataSource(0, 4);
		dataSource.setValidationQuery("SELECT FROM FOO");
		assertThat(new TomcatDataSourcePoolMetadata(dataSource).getValidationQuery()).isEqualTo("SELECT FROM FOO");
	}

	@Override
	public void getDefaultAutoCommit() {
		DataSource dataSource = createDataSource(0, 4);
		dataSource.setDefaultAutoCommit(false);
		assertThat(new TomcatDataSourcePoolMetadata(dataSource).getDefaultAutoCommit()).isFalse();
	}

	private DataSource createDataSource(int minSize, int maxSize) {
		DataSource dataSource = initializeBuilder().type(DataSource.class).build();
		dataSource.setMinIdle(minSize);
		dataSource.setMaxActive(maxSize);
		dataSource.setMinEvictableIdleTimeMillis(5000);

		// Avoid warnings
		dataSource.setInitialSize(minSize);
		dataSource.setMaxIdle(maxSize);
		return dataSource;
	}

}
