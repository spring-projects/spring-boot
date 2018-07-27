/*
 * Copyright 2012-2018 the original author or authors.
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

import java.time.Instant;
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
import org.springframework.context.ApplicationContext;

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

	private final ApplicationContext context;

	public FlywayEndpoint(ApplicationContext context) {
		this.context = context;
	}

	@ReadOperation
	public ApplicationFlywayBeans flywayBeans() {
		ApplicationContext target = this.context;
		Map<String, ContextFlywayBeans> contextFlywayBeans = new HashMap<>();
		while (target != null) {
			Map<String, FlywayDescriptor> flywayBeans = new HashMap<>();
			target.getBeansOfType(Flyway.class).forEach((name, flyway) -> flywayBeans
					.put(name, new FlywayDescriptor(flyway.info().all())));
			ApplicationContext parent = target.getParent();
			contextFlywayBeans.put(target.getId(), new ContextFlywayBeans(flywayBeans,
					(parent != null) ? parent.getId() : null));
			target = parent;
		}
		return new ApplicationFlywayBeans(contextFlywayBeans);
	}

	/**
	 * Description of an application's {@link Flyway} beans, primarily intended for
	 * serialization to JSON.
	 */
	public static final class ApplicationFlywayBeans {

		private final Map<String, ContextFlywayBeans> contexts;

		private ApplicationFlywayBeans(Map<String, ContextFlywayBeans> contexts) {
			this.contexts = contexts;
		}

		public Map<String, ContextFlywayBeans> getContexts() {
			return this.contexts;
		}

	}

	/**
	 * Description of an application context's {@link Flyway} beans, primarily intended
	 * for serialization to JSON.
	 */
	public static final class ContextFlywayBeans {

		private final Map<String, FlywayDescriptor> flywayBeans;

		private final String parentId;

		private ContextFlywayBeans(Map<String, FlywayDescriptor> flywayBeans,
				String parentId) {
			this.flywayBeans = flywayBeans;
			this.parentId = parentId;
		}

		public Map<String, FlywayDescriptor> getFlywayBeans() {
			return this.flywayBeans;
		}

		public String getParentId() {
			return this.parentId;
		}

	}

	/**
	 * Description of a {@link Flyway} bean, primarily intended for serialization to JSON.
	 */
	public static class FlywayDescriptor {

		private final List<FlywayMigration> migrations;

		private FlywayDescriptor(MigrationInfo[] migrations) {
			this.migrations = Stream.of(migrations).map(FlywayMigration::new)
					.collect(Collectors.toList());
		}

		public FlywayDescriptor(List<FlywayMigration> migrations) {
			this.migrations = migrations;
		}

		public List<FlywayMigration> getMigrations() {
			return this.migrations;
		}

	}

	/**
	 * Details of a migration performed by Flyway.
	 */
	public static final class FlywayMigration {

		private final MigrationType type;

		private final Integer checksum;

		private final String version;

		private final String description;

		private final String script;

		private final MigrationState state;

		private final String installedBy;

		private final Instant installedOn;

		private final Integer installedRank;

		private final Integer executionTime;

		private FlywayMigration(MigrationInfo info) {
			this.type = info.getType();
			this.checksum = info.getChecksum();
			this.version = nullSafeToString(info.getVersion());
			this.description = info.getDescription();
			this.script = info.getScript();
			this.state = info.getState();
			this.installedBy = info.getInstalledBy();
			this.installedOn = Instant.ofEpochMilli(info.getInstalledOn().getTime());
			this.installedRank = info.getInstalledRank();
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

		public String getInstalledBy() {
			return this.installedBy;
		}

		public Instant getInstalledOn() {
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
