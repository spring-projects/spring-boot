/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.autoconfigure;

import org.jspecify.annotations.Nullable;

/**
 * Adapts {@link DataSourceProperties} to {@link JdbcConnectionDetails}.
 *
 * @author Andy Wilkinson
 */
final class PropertiesJdbcConnectionDetails implements JdbcConnectionDetails {

	private final DataSourceProperties properties;

	PropertiesJdbcConnectionDetails(DataSourceProperties properties) {
		this.properties = properties;
	}

	@Override
	public @Nullable String getUsername() {
		return this.properties.determineUsername();
	}

	@Override
	public @Nullable String getPassword() {
		return this.properties.determinePassword();
	}

	@Override
	public String getJdbcUrl() {
		return this.properties.determineUrl();
	}

	@Override
	public String getDriverClassName() {
		return this.properties.determineDriverClassName();
	}

	@Override
	public @Nullable String getXaDataSourceClassName() {
		return (this.properties.getXa().getDataSourceClassName() != null)
				? this.properties.getXa().getDataSourceClassName()
				: JdbcConnectionDetails.super.getXaDataSourceClassName();
	}

}
