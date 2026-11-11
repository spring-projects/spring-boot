/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.metadata;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;

/**
 * A {@link DataSourcePoolMetadataProvider} implementation that returns the first
 * {@link DataSourcePoolMetadata} that is found by one of its delegate.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class CompositeDataSourcePoolMetadataProvider implements DataSourcePoolMetadataProvider {

	private final List<DataSourcePoolMetadataProvider> providers;

	/**
	 * Create a {@link CompositeDataSourcePoolMetadataProvider} instance with an initial
	 * collection of delegates to use.
	 * @param providers the data source pool metadata providers
	 */
	public CompositeDataSourcePoolMetadataProvider(
			@Nullable Collection<? extends DataSourcePoolMetadataProvider> providers) {
		this.providers = (providers != null) ? List.copyOf(providers) : Collections.emptyList();
	}

	@Override
	public @Nullable DataSourcePoolMetadata getDataSourcePoolMetadata(DataSource dataSource) {
		for (DataSourcePoolMetadataProvider provider : this.providers) {
			DataSourcePoolMetadata metadata = provider.getDataSourcePoolMetadata(dataSource);
			if (metadata != null) {
				return metadata;
			}
		}
		return null;
	}

}
