/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.metadata;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;

/**
 * A base {@link DataSourcePoolMetadata} implementation.
 *
 * @param <T> the data source type
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public abstract class AbstractDataSourcePoolMetadata<T extends DataSource> implements DataSourcePoolMetadata {

	private final T dataSource;

	/**
	 * Create an instance with the data source to use.
	 * @param dataSource the data source
	 */
	protected AbstractDataSourcePoolMetadata(T dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public @Nullable Float getUsage() {
		Integer maxSize = getMax();
		Integer currentSize = getActive();
		if (maxSize == null || currentSize == null) {
			return null;
		}
		if (maxSize < 0) {
			return -1f;
		}
		if (currentSize == 0) {
			return 0f;
		}
		return (float) currentSize / (float) maxSize;
	}

	protected final T getDataSource() {
		return this.dataSource;
	}

}
