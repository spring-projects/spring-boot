/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.flyway;

import java.sql.DatabaseMetaData;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.database.sqlserver.SQLServerConfigurationExtension;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Maps {@link FlywayProperties} to a Flyway {@link FluentConfiguration}.
 *
 * @author Moritz Halbritter
 * @since 3.0.0
 */
public class FlywayPropertiesMapper {

	/**
	 * Maps {@link FlywayProperties} to a Flyway {@link FluentConfiguration}.
	 * @param properties properties to map from
	 * @param configuration flyway configuration to map to
	 */
	public void map(FlywayProperties properties, FluentConfiguration configuration) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		String[] locations = new LocationResolver(configuration.getDataSource())
				.resolveLocations(properties.getLocations()).toArray(new String[0]);
		map.from(properties.isFailOnMissingLocations()).to(configuration::failOnMissingLocations);
		map.from(locations).to(configuration::locations);
		map.from(properties.getEncoding()).to(configuration::encoding);
		map.from(properties.getConnectRetries()).to(configuration::connectRetries);
		map.from(properties.getConnectRetriesInterval()).as(Duration::getSeconds).as(Long::intValue)
				.to(configuration::connectRetriesInterval);
		map.from(properties.getLockRetryCount()).to(configuration::lockRetryCount);
		map.from(properties.getDefaultSchema()).to(configuration::defaultSchema);
		map.from(properties.getSchemas()).as(StringUtils::toStringArray).to(configuration::schemas);
		map.from(properties.isCreateSchemas()).to(configuration::createSchemas);
		map.from(properties.getTable()).to(configuration::table);
		map.from(properties.getTablespace()).to(configuration::tablespace);
		map.from(properties.getBaselineDescription()).to(configuration::baselineDescription);
		map.from(properties.getBaselineVersion()).to(configuration::baselineVersion);
		map.from(properties.getInstalledBy()).to(configuration::installedBy);
		map.from(properties.getPlaceholders()).to(configuration::placeholders);
		map.from(properties.getPlaceholderPrefix()).to(configuration::placeholderPrefix);
		map.from(properties.getPlaceholderSuffix()).to(configuration::placeholderSuffix);
		map.from(properties.getPlaceholderSeparator()).to(configuration::placeholderSeparator);
		map.from(properties.isPlaceholderReplacement()).to(configuration::placeholderReplacement);
		map.from(properties.getSqlMigrationPrefix()).to(configuration::sqlMigrationPrefix);
		map.from(properties.getSqlMigrationSuffixes()).as(StringUtils::toStringArray)
				.to(configuration::sqlMigrationSuffixes);
		map.from(properties.getSqlMigrationSeparator()).to(configuration::sqlMigrationSeparator);
		map.from(properties.getRepeatableSqlMigrationPrefix()).to(configuration::repeatableSqlMigrationPrefix);
		map.from(properties.getTarget()).to(configuration::target);
		map.from(properties.isBaselineOnMigrate()).to(configuration::baselineOnMigrate);
		map.from(properties.isCleanDisabled()).to(configuration::cleanDisabled);
		map.from(properties.isCleanOnValidationError()).to(configuration::cleanOnValidationError);
		map.from(properties.isGroup()).to(configuration::group);
		map.from(properties.isMixed()).to(configuration::mixed);
		map.from(properties.isOutOfOrder()).to(configuration::outOfOrder);
		map.from(properties.isSkipDefaultCallbacks()).to(configuration::skipDefaultCallbacks);
		map.from(properties.isSkipDefaultResolvers()).to(configuration::skipDefaultResolvers);
		map.from(properties.isValidateMigrationNaming()).to(configuration::validateMigrationNaming);
		map.from(properties.isValidateOnMigrate()).to(configuration::validateOnMigrate);
		map.from(properties.getInitSqls()).whenNot(CollectionUtils::isEmpty)
				.as((initSqls) -> StringUtils.collectionToDelimitedString(initSqls, "\n")).to(configuration::initSql);
		map.from(properties.getScriptPlaceholderPrefix()).to(configuration::scriptPlaceholderPrefix);
		map.from(properties.getScriptPlaceholderSuffix()).to(configuration::scriptPlaceholderSuffix);
		// Flyway Teams properties
		map.from(properties.getBatch()).to(configuration::batch);
		map.from(properties.getDryRunOutput()).to(configuration::dryRunOutput);
		map.from(properties.getErrorOverrides()).to(configuration::errorOverrides);
		map.from(properties.getLicenseKey()).to(configuration::licenseKey);
		map.from(properties.getOracleSqlplus()).to(configuration::oracleSqlplus);
		map.from(properties.getOracleSqlplusWarn()).to(configuration::oracleSqlplusWarn);
		map.from(properties.getStream()).to(configuration::stream);
		map.from(properties.getUndoSqlMigrationPrefix()).to(configuration::undoSqlMigrationPrefix);
		map.from(properties.getCherryPick()).to(configuration::cherryPick);
		map.from(properties.getJdbcProperties()).whenNot(Map::isEmpty).to(configuration::jdbcProperties);
		map.from(properties.getKerberosConfigFile()).to(configuration::kerberosConfigFile);
		map.from(properties.getOracleKerberosCacheFile()).to(configuration::oracleKerberosCacheFile);
		map.from(properties.getOutputQueryResults()).to(configuration::outputQueryResults);
		map.from(properties.getSqlServerKerberosLoginFile()).whenNonNull()
				.to((sqlServerKerberosLoginFile) -> configureSqlServerKerberosLoginFile(configuration,
						sqlServerKerberosLoginFile));
		map.from(properties.getSkipExecutingMigrations()).to(configuration::skipExecutingMigrations);
		map.from(properties.getIgnoreMigrationPatterns()).whenNot(List::isEmpty)
				.as((patterns) -> patterns.toArray(new String[0])).to(configuration::ignoreMigrationPatterns);
		map.from(properties.getDetectEncoding()).to(configuration::detectEncoding);
	}

	private void configureSqlServerKerberosLoginFile(FluentConfiguration configuration,
			String sqlServerKerberosLoginFile) {
		SQLServerConfigurationExtension sqlServerConfigurationExtension = configuration.getPluginRegister()
				.getPlugin(SQLServerConfigurationExtension.class);
		Assert.state(sqlServerConfigurationExtension != null, "Flyway SQL Server extension missing");
		sqlServerConfigurationExtension.setKerberosLoginFile(sqlServerKerberosLoginFile);
	}

	private static class LocationResolver {

		private static final String VENDOR_PLACEHOLDER = "{vendor}";

		private final DataSource dataSource;

		LocationResolver(DataSource dataSource) {
			this.dataSource = dataSource;
		}

		List<String> resolveLocations(List<String> locations) {
			if (usesVendorLocation(locations)) {
				DatabaseDriver databaseDriver = getDatabaseDriver();
				return replaceVendorLocations(locations, databaseDriver);
			}
			return locations;
		}

		private List<String> replaceVendorLocations(List<String> locations, DatabaseDriver databaseDriver) {
			if (databaseDriver == DatabaseDriver.UNKNOWN) {
				return locations;
			}
			String vendor = databaseDriver.getId();
			return locations.stream().map((location) -> location.replace(VENDOR_PLACEHOLDER, vendor)).toList();
		}

		private DatabaseDriver getDatabaseDriver() {
			try {
				String url = JdbcUtils.extractDatabaseMetaData(this.dataSource, DatabaseMetaData::getURL);
				return DatabaseDriver.fromJdbcUrl(url);
			}
			catch (MetaDataAccessException ex) {
				throw new IllegalStateException(ex);
			}

		}

		private boolean usesVendorLocation(Collection<String> locations) {
			for (String location : locations) {
				if (location.contains(VENDOR_PLACEHOLDER)) {
					return true;
				}
			}
			return false;
		}

	}

}
