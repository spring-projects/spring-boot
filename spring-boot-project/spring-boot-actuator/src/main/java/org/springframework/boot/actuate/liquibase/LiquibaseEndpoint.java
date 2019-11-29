/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.liquibase;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import liquibase.changelog.ChangeSet.ExecType;
import liquibase.changelog.RanChangeSet;
import liquibase.changelog.StandardChangeLogHistoryService;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.integration.spring.SpringLiquibase;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link Endpoint @Endpoint} to expose liquibase info.
 *
 * @author Eddú Meléndez
 * @since 2.0.0
 */
@Endpoint(id = "liquibase")
public class LiquibaseEndpoint {

	private final ApplicationContext context;

	public LiquibaseEndpoint(ApplicationContext context) {
		Assert.notNull(context, "Context must be specified");
		this.context = context;
	}

	@ReadOperation
	public ApplicationLiquibaseBeans liquibaseBeans() {
		ApplicationContext target = this.context;
		Map<String, ContextLiquibaseBeans> contextBeans = new HashMap<>();
		while (target != null) {
			Map<String, LiquibaseBean> liquibaseBeans = new HashMap<>();
			DatabaseFactory factory = DatabaseFactory.getInstance();
			this.context.getBeansOfType(SpringLiquibase.class)
					.forEach((name, liquibase) -> liquibaseBeans.put(name, createReport(liquibase, factory)));
			ApplicationContext parent = target.getParent();
			contextBeans.put(target.getId(),
					new ContextLiquibaseBeans(liquibaseBeans, (parent != null) ? parent.getId() : null));
			target = parent;
		}
		return new ApplicationLiquibaseBeans(contextBeans);
	}

	private LiquibaseBean createReport(SpringLiquibase liquibase, DatabaseFactory factory) {
		try {
			DataSource dataSource = liquibase.getDataSource();
			JdbcConnection connection = new JdbcConnection(dataSource.getConnection());
			Database database = null;
			try {
				database = factory.findCorrectDatabaseImplementation(connection);
				String defaultSchema = liquibase.getDefaultSchema();
				if (StringUtils.hasText(defaultSchema)) {
					database.setDefaultSchemaName(defaultSchema);
				}
				database.setDatabaseChangeLogTableName(liquibase.getDatabaseChangeLogTable());
				database.setDatabaseChangeLogLockTableName(liquibase.getDatabaseChangeLogLockTable());
				StandardChangeLogHistoryService service = new StandardChangeLogHistoryService();
				service.setDatabase(database);
				return new LiquibaseBean(
						service.getRanChangeSets().stream().map(ChangeSet::new).collect(Collectors.toList()));
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
	 * Description of an application's {@link SpringLiquibase} beans, primarily intended
	 * for serialization to JSON.
	 */
	public static final class ApplicationLiquibaseBeans {

		private final Map<String, ContextLiquibaseBeans> contexts;

		private ApplicationLiquibaseBeans(Map<String, ContextLiquibaseBeans> contexts) {
			this.contexts = contexts;
		}

		public Map<String, ContextLiquibaseBeans> getContexts() {
			return this.contexts;
		}

	}

	/**
	 * Description of an application context's {@link SpringLiquibase} beans, primarily
	 * intended for serialization to JSON.
	 */
	public static final class ContextLiquibaseBeans {

		private final Map<String, LiquibaseBean> liquibaseBeans;

		private final String parentId;

		private ContextLiquibaseBeans(Map<String, LiquibaseBean> liquibaseBeans, String parentId) {
			this.liquibaseBeans = liquibaseBeans;
			this.parentId = parentId;
		}

		public Map<String, LiquibaseBean> getLiquibaseBeans() {
			return this.liquibaseBeans;
		}

		public String getParentId() {
			return this.parentId;
		}

	}

	/**
	 * Description of a {@link SpringLiquibase} bean, primarily intended for serialization
	 * to JSON.
	 */
	public static final class LiquibaseBean {

		private final List<ChangeSet> changeSets;

		public LiquibaseBean(List<ChangeSet> changeSets) {
			this.changeSets = changeSets;
		}

		public List<ChangeSet> getChangeSets() {
			return this.changeSets;
		}

	}

	/**
	 * A Liquibase change set.
	 */
	public static class ChangeSet {

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

		private final String checksum;

		private final Integer orderExecuted;

		private final String tag;

		public ChangeSet(RanChangeSet ranChangeSet) {
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

		public String getChecksum() {
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
	 * A context expression in a {@link ChangeSet}.
	 */
	public static class ContextExpression {

		private final Set<String> contexts;

		public ContextExpression(Set<String> contexts) {
			this.contexts = contexts;
		}

		public Set<String> getContexts() {
			return this.contexts;
		}

	}

}
