/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.jdbc.init;

import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.sql.DataSource;

import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Utility class that can resolve placeholder text with the actual {@link DatabaseDriver}
 * platform.
 * <p>
 * By default, the name of the platform is the {@link DatabaseDriver#getId ID of the
 * driver}. This mapping can be customized by
 * {@link #withDriverPlatform(DatabaseDriver, String)} registering custom
 * {@code DatabaseDriver} to platform mappings.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 2.6.0
 */
public class PlatformPlaceholderDatabaseDriverResolver {

	private final String placeholder;

	private final Map<DatabaseDriver, String> driverMappings;

	/**
	 * Creates a new resolver that will use the default {@code "@@platform@@"}
	 * placeholder.
	 */
	public PlatformPlaceholderDatabaseDriverResolver() {
		this("@@platform@@");
	}

	/**
	 * Creates a new resolver that will use the given {@code placeholder}.
	 * @param placeholder the placeholder to use
	 */
	public PlatformPlaceholderDatabaseDriverResolver(String placeholder) {
		this(placeholder, Collections.emptyMap());
	}

	/**
     * Constructs a new PlatformPlaceholderDatabaseDriverResolver with the specified placeholder and driver mappings.
     * 
     * @param placeholder the placeholder to be used for resolving the database driver
     * @param driverMappings a map containing the database drivers and their corresponding placeholders
     */
    private PlatformPlaceholderDatabaseDriverResolver(String placeholder, Map<DatabaseDriver, String> driverMappings) {
		this.placeholder = placeholder;
		this.driverMappings = driverMappings;
	}

	/**
	 * Creates a new {@link PlatformPlaceholderDatabaseDriverResolver} that will map the
	 * given {@code driver} to the given {@code platform}.
	 * @param driver the driver
	 * @param platform the platform
	 * @return the new resolver
	 */
	public PlatformPlaceholderDatabaseDriverResolver withDriverPlatform(DatabaseDriver driver, String platform) {
		Map<DatabaseDriver, String> driverMappings = new LinkedHashMap<>(this.driverMappings);
		driverMappings.put(driver, platform);
		return new PlatformPlaceholderDatabaseDriverResolver(this.placeholder, driverMappings);
	}

	/**
	 * Resolves the placeholders in the given {@code values}, replacing them with the
	 * platform derived from the {@link DatabaseDriver} of the given {@code dataSource}.
	 * @param dataSource the DataSource from which the {@link DatabaseDriver} is derived
	 * @param values the values in which placeholders are resolved
	 * @return the values with their placeholders resolved
	 */
	public List<String> resolveAll(DataSource dataSource, String... values) {
		Assert.notNull(dataSource, "DataSource must not be null");
		return resolveAll(() -> determinePlatform(dataSource), values);
	}

	/**
	 * Resolves the placeholders in the given {@code values}, replacing them with the
	 * given platform.
	 * @param platform the platform to use
	 * @param values the values in which placeholders are resolved
	 * @return the values with their placeholders resolved
	 * @since 2.6.2
	 */
	public List<String> resolveAll(String platform, String... values) {
		Assert.notNull(platform, "Platform must not be null");
		return resolveAll(() -> platform, values);
	}

	/**
     * Resolves all the values in the given array by replacing the placeholder with the platform provided by the platformProvider.
     * 
     * @param platformProvider a supplier that provides the platform value
     * @param values the array of values to be resolved
     * @return a list of resolved values
     */
    private List<String> resolveAll(Supplier<String> platformProvider, String... values) {
		if (ObjectUtils.isEmpty(values)) {
			return Collections.emptyList();
		}
		List<String> resolved = new ArrayList<>(values.length);
		String platform = null;
		for (String value : values) {
			if (StringUtils.hasLength(value)) {
				if (value.contains(this.placeholder)) {
					platform = (platform != null) ? platform : platformProvider.get();
					value = value.replace(this.placeholder, platform);
				}
			}
			resolved.add(value);
		}
		return Collections.unmodifiableList(resolved);
	}

	/**
     * Determines the platform of the given data source.
     * 
     * @param dataSource the data source to determine the platform for
     * @return the platform of the data source
     * @throws IllegalStateException if the database type cannot be detected
     */
    private String determinePlatform(DataSource dataSource) {
		DatabaseDriver databaseDriver = getDatabaseDriver(dataSource);
		Assert.state(databaseDriver != DatabaseDriver.UNKNOWN, "Unable to detect database type");
		return this.driverMappings.getOrDefault(databaseDriver, databaseDriver.getId());
	}

	/**
     * Retrieves the DatabaseDriver based on the provided DataSource.
     * 
     * @param dataSource the DataSource to retrieve the DatabaseDriver from
     * @return the DatabaseDriver associated with the DataSource
     * @throws IllegalStateException if failed to determine the DatabaseDriver
     */
    DatabaseDriver getDatabaseDriver(DataSource dataSource) {
		try {
			String productName = JdbcUtils.commonDatabaseName(
					JdbcUtils.extractDatabaseMetaData(dataSource, DatabaseMetaData::getDatabaseProductName));
			return DatabaseDriver.fromProductName(productName);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to determine DatabaseDriver", ex);
		}
	}

}
