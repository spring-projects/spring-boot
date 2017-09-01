/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.flyway;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.flywaydb.core.api.MigrationType;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.util.Assert;

/**
 * {@link Endpoint} to expose flyway info.
 *
 * @author Eddú Meléndez
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 2.0.0
 */
@Endpoint(id = "flyway")
public class FlywayEndpoint {

	private final Map<String, Flyway> flywayBeans;

	public FlywayEndpoint(Map<String, Flyway> flywayBeans) {
		Assert.notEmpty(flywayBeans, "FlywayBeans must be specified");
		this.flywayBeans = flywayBeans;
	}

	@ReadOperation
	public Map<String, FlywayReport> flywayReports() {
		Map<String, FlywayReport> reports = new HashMap<>();
		this.flywayBeans.forEach((name, flyway) -> reports.put(name,
				new FlywayReport(flyway.info().all())));
		return reports;
	}

	/**
	 * Report for one {@link Flyway} instance.
	 */
	public static class FlywayReport {

		private final List<FlywayMigration> migrations;

		public FlywayReport(MigrationInfo[] migrations) {
			this.migrations = Stream.of(migrations).map(FlywayMigration::new)
					.collect(Collectors.toList());
		}

		public FlywayReport(List<FlywayMigration> migrations) {
			this.migrations = migrations;
		}

		public List<FlywayMigration> getMigrations() {
			return this.migrations;
		}

	}

	/**
	 * Details of a migration performed by Flyway.
	 */
	public static class FlywayMigration {

		private final MigrationType type;

		private final Integer checksum;

		private final String version;

		private final String description;

		private final String script;

		private final MigrationState state;

		private final String installedBy;

		private final Date installedOn;

		private final Integer installedRank;

		private final Integer executionTime;

		public FlywayMigration(MigrationInfo info) {
			this.type = info.getType();
			this.checksum = info.getChecksum();
			this.version = nullSafeToString(info.getVersion());
			this.description = info.getDescription();
			this.script = info.getScript();
			this.state = info.getState();
			this.installedBy = info.getInstalledBy();
			this.installedOn = info.getInstalledOn();
			this.installedRank = info.getInstalledRank();
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

		public String getInstalledBy() {
			return this.installedBy;
		}

		public Date getInstalledOn() {
			return this.installedOn;
		}

		public Integer getInstalledRank() {
			return this.installedRank;
		}

		public Integer getExecutionTime() {
			return this.executionTime;
		}

	}

}
