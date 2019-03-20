/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.flywaydb.core.api.MigrationType;

import org.springframework.boot.actuate.endpoint.FlywayEndpoint.FlywayReport;
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
public class FlywayEndpoint extends AbstractEndpoint<List<FlywayReport>> {

	private final Map<String, Flyway> flyways;

	public FlywayEndpoint(Flyway flyway) {
		this(Collections.singletonMap("default", flyway));
	}

	public FlywayEndpoint(Map<String, Flyway> flyways) {
		super("flyway");
		Assert.notEmpty(flyways, "Flyways must be specified");
		this.flyways = flyways;
	}

	@Override
	public List<FlywayReport> invoke() {
		List<FlywayReport> reports = new ArrayList<FlywayReport>();
		for (Map.Entry<String, Flyway> entry : this.flyways.entrySet()) {
			List<FlywayMigration> migrations = new ArrayList<FlywayMigration>();
			for (MigrationInfo info : entry.getValue().info().all()) {
				migrations.add(new FlywayMigration(info));
			}
			reports.add(new FlywayReport(entry.getKey(), migrations));
		}
		return reports;
	}

	/**
	 * Flyway report for one datasource.
	 */
	public static class FlywayReport {

		private final String name;

		private final List<FlywayMigration> migrations;

		public FlywayReport(String name, List<FlywayMigration> migrations) {
			this.name = name;
			this.migrations = migrations;
		}

		public String getName() {
			return this.name;
		}

		public List<FlywayMigration> getMigrations() {
			return this.migrations;
		}

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
			return (obj != null) ? obj.toString() : null;
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
