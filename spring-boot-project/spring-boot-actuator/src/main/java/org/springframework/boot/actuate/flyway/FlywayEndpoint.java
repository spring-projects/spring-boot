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

package org.springframework.boot.actuate.flyway;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;

import org.springframework.boot.actuate.endpoint.OperationResponseBody;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.context.ApplicationContext;

/**
 * {@link Endpoint @Endpoint} to expose flyway info.
 *
 * @author Eddú Meléndez
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Artsiom Yudovin
 * @since 2.0.0
 */
@Endpoint(id = "flyway")
public class FlywayEndpoint {

	private final ApplicationContext context;

	/**
	 * Creates a new instance of FlywayEndpoint with the specified ApplicationContext.
	 * @param context the ApplicationContext to be used by the FlywayEndpoint
	 */
	public FlywayEndpoint(ApplicationContext context) {
		this.context = context;
	}

	/**
	 * Retrieves the Flyway beans descriptor for the current application context and its
	 * parent contexts.
	 * @return The Flyway beans descriptor containing information about Flyway beans in
	 * the application context hierarchy.
	 */
	@ReadOperation
	public FlywayBeansDescriptor flywayBeans() {
		ApplicationContext target = this.context;
		Map<String, ContextFlywayBeansDescriptor> contextFlywayBeans = new HashMap<>();
		while (target != null) {
			Map<String, FlywayDescriptor> flywayBeans = new HashMap<>();
			target.getBeansOfType(Flyway.class)
				.forEach((name, flyway) -> flywayBeans.put(name, new FlywayDescriptor(flyway.info().all())));
			ApplicationContext parent = target.getParent();
			contextFlywayBeans.put(target.getId(),
					new ContextFlywayBeansDescriptor(flywayBeans, (parent != null) ? parent.getId() : null));
			target = parent;
		}
		return new FlywayBeansDescriptor(contextFlywayBeans);
	}

	/**
	 * Description of an application's {@link Flyway} beans.
	 */
	public static final class FlywayBeansDescriptor implements OperationResponseBody {

		private final Map<String, ContextFlywayBeansDescriptor> contexts;

		/**
		 * Constructs a new FlywayBeansDescriptor with the specified contexts.
		 * @param contexts a map of contexts and their corresponding
		 * ContextFlywayBeansDescriptor objects
		 */
		private FlywayBeansDescriptor(Map<String, ContextFlywayBeansDescriptor> contexts) {
			this.contexts = contexts;
		}

		/**
		 * Returns the map of contexts and their corresponding FlywayBeansDescriptor
		 * objects.
		 * @return the map of contexts and their corresponding FlywayBeansDescriptor
		 * objects
		 */
		public Map<String, ContextFlywayBeansDescriptor> getContexts() {
			return this.contexts;
		}

	}

	/**
	 * Description of an application context's {@link Flyway} beans.
	 */
	public static final class ContextFlywayBeansDescriptor {

		private final Map<String, FlywayDescriptor> flywayBeans;

		private final String parentId;

		/**
		 * Constructs a new ContextFlywayBeansDescriptor with the specified flywayBeans
		 * and parentId.
		 * @param flywayBeans the map of flyway beans
		 * @param parentId the parent ID
		 */
		private ContextFlywayBeansDescriptor(Map<String, FlywayDescriptor> flywayBeans, String parentId) {
			this.flywayBeans = flywayBeans;
			this.parentId = parentId;
		}

		/**
		 * Returns the map of Flyway beans.
		 * @return the map of Flyway beans
		 */
		public Map<String, FlywayDescriptor> getFlywayBeans() {
			return this.flywayBeans;
		}

		/**
		 * Returns the parent ID of the ContextFlywayBeansDescriptor.
		 * @return the parent ID of the ContextFlywayBeansDescriptor
		 */
		public String getParentId() {
			return this.parentId;
		}

	}

	/**
	 * Description of a {@link Flyway} bean.
	 */
	public static class FlywayDescriptor {

		private final List<FlywayMigrationDescriptor> migrations;

		/**
		 * Constructs a new FlywayDescriptor object with the given array of MigrationInfo
		 * objects.
		 * @param migrations an array of MigrationInfo objects representing the migrations
		 */
		private FlywayDescriptor(MigrationInfo[] migrations) {
			this.migrations = Stream.of(migrations).map(FlywayMigrationDescriptor::new).toList();
		}

		/**
		 * Constructs a new FlywayDescriptor object with the specified list of migrations.
		 * @param migrations the list of FlywayMigrationDescriptor objects representing
		 * the migrations
		 */
		public FlywayDescriptor(List<FlywayMigrationDescriptor> migrations) {
			this.migrations = migrations;
		}

		/**
		 * Returns the list of FlywayMigrationDescriptor objects.
		 * @return the list of FlywayMigrationDescriptor objects
		 */
		public List<FlywayMigrationDescriptor> getMigrations() {
			return this.migrations;
		}

	}

	/**
	 * Description of a migration performed by Flyway.
	 */
	public static final class FlywayMigrationDescriptor {

		private final String type;

		private final Integer checksum;

		private final String version;

		private final String description;

		private final String script;

		private final MigrationState state;

		private final String installedBy;

		private final Instant installedOn;

		private final Integer installedRank;

		private final Integer executionTime;

		/**
		 * Constructs a new FlywayMigrationDescriptor object based on the provided
		 * MigrationInfo.
		 * @param info the MigrationInfo object to create the descriptor from
		 */
		private FlywayMigrationDescriptor(MigrationInfo info) {
			this.type = info.getType().name();
			this.checksum = info.getChecksum();
			this.version = nullSafeToString(info.getVersion());
			this.description = info.getDescription();
			this.script = info.getScript();
			this.state = info.getState();
			this.installedBy = info.getInstalledBy();
			this.installedRank = info.getInstalledRank();
			this.executionTime = info.getExecutionTime();
			this.installedOn = nullSafeToInstant(info.getInstalledOn());
		}

		/**
		 * Returns a string representation of the given object, handling null values.
		 * @param obj the object to convert to a string
		 * @return the string representation of the object, or null if the object is null
		 */
		private String nullSafeToString(Object obj) {
			return (obj != null) ? obj.toString() : null;
		}

		/**
		 * Converts a {@link Date} object to an {@link Instant} object, handling null
		 * values.
		 * @param date the {@link Date} object to convert
		 * @return the converted {@link Instant} object, or null if the input is null
		 */
		private Instant nullSafeToInstant(Date date) {
			return (date != null) ? Instant.ofEpochMilli(date.getTime()) : null;
		}

		/**
		 * Returns the type of the Flyway migration descriptor.
		 * @return the type of the Flyway migration descriptor
		 */
		public String getType() {
			return this.type;
		}

		/**
		 * Returns the checksum of the Flyway migration descriptor.
		 * @return the checksum of the Flyway migration descriptor
		 */
		public Integer getChecksum() {
			return this.checksum;
		}

		/**
		 * Returns the version of the FlywayMigrationDescriptor.
		 * @return the version of the FlywayMigrationDescriptor
		 */
		public String getVersion() {
			return this.version;
		}

		/**
		 * Returns the description of the FlywayMigrationDescriptor.
		 * @return the description of the FlywayMigrationDescriptor
		 */
		public String getDescription() {
			return this.description;
		}

		/**
		 * Returns the script associated with this FlywayMigrationDescriptor.
		 * @return the script associated with this FlywayMigrationDescriptor
		 */
		public String getScript() {
			return this.script;
		}

		/**
		 * Returns the current state of the migration.
		 * @return the current state of the migration
		 */
		public MigrationState getState() {
			return this.state;
		}

		/**
		 * Returns the name of the user who installed the Flyway migration.
		 * @return the name of the user who installed the Flyway migration
		 */
		public String getInstalledBy() {
			return this.installedBy;
		}

		/**
		 * Returns the date and time when the Flyway migration was installed.
		 * @return the date and time when the Flyway migration was installed
		 */
		public Instant getInstalledOn() {
			return this.installedOn;
		}

		/**
		 * Returns the installed rank of the Flyway migration descriptor.
		 * @return the installed rank of the Flyway migration descriptor
		 */
		public Integer getInstalledRank() {
			return this.installedRank;
		}

		/**
		 * Returns the execution time of the Flyway migration.
		 * @return the execution time of the Flyway migration
		 */
		public Integer getExecutionTime() {
			return this.executionTime;
		}

	}

}
