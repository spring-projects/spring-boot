/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.liquibase.endpoint;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import liquibase.changelog.ChangeSet.ExecType;
import liquibase.changelog.RanChangeSet;
import liquibase.changelog.StandardChangeLogHistoryService;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.integration.spring.SpringLiquibase;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.actuate.endpoint.OperationResponseBody;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link Endpoint @Endpoint} to expose liquibase info.
 *
 * @author Eddú Meléndez
 * @author Nabil Fawwaz Elqayyim
 * @since 4.0.0
 */
@Endpoint(id = "liquibase")
public class LiquibaseEndpoint {

	private final ApplicationContext context;

	public LiquibaseEndpoint(ApplicationContext context) {
		Assert.notNull(context, "'context' must be specified");
		this.context = context;
	}

	@ReadOperation
	public LiquibaseBeansDescriptor liquibaseBeans() {
		ApplicationContext target = this.context;
		Map<@Nullable String, ContextLiquibaseBeansDescriptor> contextBeans = new HashMap<>();
		while (target != null) {
			Map<String, LiquibaseBeanDescriptor> liquibaseBeans = new HashMap<>();
			DatabaseFactory factory = DatabaseFactory.getInstance();
			target.getBeansOfType(SpringLiquibase.class)
				.forEach((name, liquibase) -> liquibaseBeans.put(name, createReport(liquibase, factory)));
			ApplicationContext parent = target.getParent();
			contextBeans.put(target.getId(),
					new ContextLiquibaseBeansDescriptor(liquibaseBeans, (parent != null) ? parent.getId() : null));
			target = parent;
		}
		return new LiquibaseBeansDescriptor(contextBeans);
	}

	private LiquibaseBeanDescriptor createReport(SpringLiquibase liquibase, DatabaseFactory factory) {
		try {
			DataSource dataSource = liquibase.getDataSource();
			JdbcConnection connection = new JdbcConnection(dataSource.getConnection());
			Database database = null;
			try {
				database = factory.findCorrectDatabaseImplementation(connection);
				String schemaToUse = liquibase.getLiquibaseSchema();
				if (!StringUtils.hasText(schemaToUse)) { // Use liquibase-schema if set, otherwise fall back to default-schema
					schemaToUse = liquibase.getDefaultSchema();
				}
				if (StringUtils.hasText(schemaToUse)) {
					database.setDefaultSchemaName(schemaToUse);
				}
				database.setDatabaseChangeLogTableName(liquibase.getDatabaseChangeLogTable());
				database.setDatabaseChangeLogLockTableName(liquibase.getDatabaseChangeLogLockTable());
				StandardChangeLogHistoryService service = new StandardChangeLogHistoryService();
				service.setDatabase(database);
				return new LiquibaseBeanDescriptor(
						service.getRanChangeSets().stream().map(ChangeSetDescriptor::new).toList());
			}
			finally {
				if (database != null) {
					database.close();
				}
				else {
					connection.close();
				}
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to get Liquibase change sets", ex);
		}
	}

	/**
	 * Description of an application's {@link SpringLiquibase} beans.
	 */
	public static final class LiquibaseBeansDescriptor implements OperationResponseBody {

		private final Map<@Nullable String, ContextLiquibaseBeansDescriptor> contexts;

		private LiquibaseBeansDescriptor(Map<@Nullable String, ContextLiquibaseBeansDescriptor> contexts) {
			this.contexts = contexts;
		}

		public Map<@Nullable String, ContextLiquibaseBeansDescriptor> getContexts() {
			return this.contexts;
		}

	}

	/**
	 * Description of an application context's {@link SpringLiquibase} beans.
	 */
	public static final class ContextLiquibaseBeansDescriptor {

		private final Map<String, LiquibaseBeanDescriptor> liquibaseBeans;

		private final @Nullable String parentId;

		private ContextLiquibaseBeansDescriptor(Map<String, LiquibaseBeanDescriptor> liquibaseBeans,
				@Nullable String parentId) {
			this.liquibaseBeans = liquibaseBeans;
			this.parentId = parentId;
		}

		public Map<String, LiquibaseBeanDescriptor> getLiquibaseBeans() {
			return this.liquibaseBeans;
		}

		public @Nullable String getParentId() {
			return this.parentId;
		}

	}

	/**
	 * Description of a {@link SpringLiquibase} bean.
	 */
	public static final class LiquibaseBeanDescriptor {

		private final List<ChangeSetDescriptor> changeSets;

		public LiquibaseBeanDescriptor(List<ChangeSetDescriptor> changeSets) {
			this.changeSets = changeSets;
		}

		public List<ChangeSetDescriptor> getChangeSets() {
			return this.changeSets;
		}

	}

	/**
	 * Description of a Liquibase change set.
	 */
	public static class ChangeSetDescriptor {

		private final String author;

		private final String changeLog;

		private final String comments;

		private final Set<String> contexts;

		private final Instant dateExecuted;

		private final String deploymentId;

		private final String description;

		private final ExecType execType;

		private final String id;

		private final Set<String> labels;

		private final @Nullable String checksum;

		private final Integer orderExecuted;

		private final String tag;

		public ChangeSetDescriptor(RanChangeSet ranChangeSet) {
			this.author = ranChangeSet.getAuthor();
			this.changeLog = ranChangeSet.getChangeLog();
			this.comments = ranChangeSet.getComments();
			this.contexts = ranChangeSet.getContextExpression().getContexts();
			this.dateExecuted = Instant.ofEpochMilli(ranChangeSet.getDateExecuted().getTime());
			this.deploymentId = ranChangeSet.getDeploymentId();
			this.description = ranChangeSet.getDescription();
			this.execType = ranChangeSet.getExecType();
			this.id = ranChangeSet.getId();
			this.labels = ranChangeSet.getLabels().getLabels();
			this.checksum = ((ranChangeSet.getLastCheckSum() != null) ? ranChangeSet.getLastCheckSum().toString()
					: null);
			this.orderExecuted = ranChangeSet.getOrderExecuted();
			this.tag = ranChangeSet.getTag();
		}

		public String getAuthor() {
			return this.author;
		}

		public String getChangeLog() {
			return this.changeLog;
		}

		public String getComments() {
			return this.comments;
		}

		public Set<String> getContexts() {
			return this.contexts;
		}

		public Instant getDateExecuted() {
			return this.dateExecuted;
		}

		public String getDeploymentId() {
			return this.deploymentId;
		}

		public String getDescription() {
			return this.description;
		}

		public ExecType getExecType() {
			return this.execType;
		}

		public String getId() {
			return this.id;
		}

		public Set<String> getLabels() {
			return this.labels;
		}

		public @Nullable String getChecksum() {
			return this.checksum;
		}

		public Integer getOrderExecuted() {
			return this.orderExecuted;
		}

		public String getTag() {
			return this.tag;
		}

	}

	/**
	 * Description of a context expression in a {@link ChangeSetDescriptor}.
	 */
	public static class ContextExpressionDescriptor {

		private final Set<String> contexts;

		public ContextExpressionDescriptor(Set<String> contexts) {
			this.contexts = contexts;
		}

		public Set<String> getContexts() {
			return this.contexts;
		}

	}

}
