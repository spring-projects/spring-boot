/*
 * Copyright 2012-2015 the original author or authors.
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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
 * @since 1.3.0
 */
@ConfigurationProperties(prefix = "endpoints.flyway", ignoreUnknownFields = true)
public class FlywayEndpoint extends AbstractEndpoint<List<FlywayMigration>> {

	private final Flyway flyway;

	public FlywayEndpoint(Flyway flyway) {
		super("flyway");
		Assert.notNull(flyway, "Flyway must not be null");
		this.flyway = flyway;
	}

	@Override
	public List<FlywayMigration> invoke() {
		List<FlywayMigration> migrations = new ArrayList<FlywayMigration>();
		for (MigrationInfo info : this.flyway.info().all()) {
			migrations.add(new FlywayMigration(info));
		}
		return migrations;
	}

	/**
	 * Migration properties.
	 */
	public static class FlywayMigration {

		private MigrationType type;

		private Integer checksum;

		private String version;

		private String description;

		private String script;

		private MigrationState state;

		private Date installedOn;

		private Integer executionTime;

		public FlywayMigration(MigrationInfo info) {
			this.type = info.getType();
			this.checksum = info.getChecksum();
			this.version = info.getVersion().toString();
			this.description = info.getDescription();
			this.script = info.getScript();
			this.state = info.getState();
			this.installedOn = info.getInstalledOn();
			this.executionTime = info.getExecutionTime();
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
