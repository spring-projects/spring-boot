/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.endpoint;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.flywaydb.core.api.MigrationType;

import org.springframework.boot.actuate.endpoint.FlywayEndpoint.FlywayMigration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

/**
 * {@link Endpoint} to expose flyway info.
 *
 * @author Eddú Meléndez
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.3.0
 */
@ConfigurationProperties(prefix = "endpoints.flyway")
public class FlywayEndpoint extends AbstractEndpoint<Map<String, List<FlywayMigration>>> {

	private final List<Flyway> flyway;

	public FlywayEndpoint(Flyway flyway) {
		this(Collections.singletonList(flyway));
	}

	public FlywayEndpoint(List<Flyway> flyway) {
		super("flyway");
		Assert.notNull(flyway, "Flyway must not be null");
		this.flyway = flyway;
	}

	@Override
	public Map<String, List<FlywayMigration>> invoke() {
		Map<String, List<FlywayMigration>> migrations = new HashMap<String, List<FlywayMigration>>();
		for (Flyway flyway : this.flyway) {
			Connection connection = null;
			try {
				connection = flyway.getDataSource().getConnection();
				DatabaseMetaData metaData = connection.getMetaData();

				List<FlywayMigration> migration = new ArrayList<FlywayMigration>();
				for (MigrationInfo info : flyway.info().all()) {
					migration.add(new FlywayMigration(info));
				}
				migrations.put(metaData.getURL(), migration);
			}
			catch (SQLException e) {
				//Continue
			}
			finally {
				try {
					connection.close();
				}
				catch (SQLException e) {
					//Continue
				}
			}
		}
		return migrations;
	}

	/**
	 * Migration properties.
	 */
	public static class FlywayMigration {

		private final MigrationType type;

		private final Integer checksum;

		private final String version;

		private final String description;

		private final String script;

		private final MigrationState state;

		private final Date installedOn;

		private final Integer executionTime;

		public FlywayMigration(MigrationInfo info) {
			this.type = info.getType();
			this.checksum = info.getChecksum();
			this.version = nullSafeToString(info.getVersion());
			this.description = info.getDescription();
			this.script = info.getScript();
			this.state = info.getState();
			this.installedOn = info.getInstalledOn();
			this.executionTime = info.getExecutionTime();
		}

		private String nullSafeToString(Object obj) {
			return (obj == null ? null : obj.toString());
		}

		public MigrationType getType() {
			return this.type;
		}

		public Integer getChecksum() {
			return this.checksum;
		}

		public String getVersion() {
			return this.version;
		}

		public String getDescription() {
			return this.description;
		}

		public String getScript() {
			return this.script;
		}

		public MigrationState getState() {
			return this.state;
		}

		public Date getInstalledOn() {
			return this.installedOn;
		}

		public Integer getExecutionTime() {
			return this.executionTime;
		}

	}

}
