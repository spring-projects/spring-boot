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

package org.springframework.boot.actuate.liquibase;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import liquibase.changelog.ChangeLogHistoryService;
import liquibase.changelog.ChangeSet.ExecType;
import liquibase.changelog.RanChangeSet;
import liquibase.changelog.StandardChangeLogHistoryService;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.integration.spring.SpringLiquibase;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link Endpoint} to expose liquibase info.
 *
 * @author Eddú Meléndez
 * @since 2.0.0
 */
@Endpoint(id = "liquibase")
public class LiquibaseEndpoint {

	private final Map<String, SpringLiquibase> liquibaseBeans;

	public LiquibaseEndpoint(Map<String, SpringLiquibase> liquibaseBeans) {
		Assert.notEmpty(liquibaseBeans, "LiquibaseBeans must be specified");
		this.liquibaseBeans = liquibaseBeans;
	}

	@ReadOperation
	public Map<String, LiquibaseReport> liquibaseReports() {
		Map<String, LiquibaseReport> reports = new HashMap<>();
		DatabaseFactory factory = DatabaseFactory.getInstance();
		StandardChangeLogHistoryService service = new StandardChangeLogHistoryService();
		this.liquibaseBeans.forEach((name, liquibase) -> reports.put(name,
				createReport(liquibase, service, factory)));
		return reports;
	}

	private LiquibaseReport createReport(SpringLiquibase liquibase,
			ChangeLogHistoryService service, DatabaseFactory factory) {
		try {
			DataSource dataSource = liquibase.getDataSource();
			JdbcConnection connection = new JdbcConnection(dataSource.getConnection());
			try {
				Database database = factory.findCorrectDatabaseImplementation(connection);
				String defaultSchema = liquibase.getDefaultSchema();
				if (StringUtils.hasText(defaultSchema)) {
					database.setDefaultSchemaName(defaultSchema);
				}
				service.setDatabase(database);
				return new LiquibaseReport(service.getRanChangeSets().stream()
						.map(ChangeSet::new).collect(Collectors.toList()));
			}
			finally {
				connection.close();
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to get Liquibase change sets", ex);
		}
	}

	/**
	 * Report for a single {@link SpringLiquibase} instance.
	 */
	public static class LiquibaseReport {

		private final List<ChangeSet> changeSets;

		public LiquibaseReport(List<ChangeSet> changeSets) {
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

		private final ContextExpression contextExpression;

		private final Date dateExecuted;

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
			this.contextExpression = new ContextExpression(
					ranChangeSet.getContextExpression().getContexts());
			this.dateExecuted = ranChangeSet.getDateExecuted();
			this.deploymentId = ranChangeSet.getDeploymentId();
			this.description = ranChangeSet.getDescription();
			this.execType = ranChangeSet.getExecType();
			this.id = ranChangeSet.getId();
			this.labels = ranChangeSet.getLabels().getLabels();
			this.checksum = ranChangeSet.getLastCheckSum() == null ? null
					: ranChangeSet.getLastCheckSum().toString();
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

		public ContextExpression getContextExpression() {
			return this.contextExpression;
		}

		public Date getDateExecuted() {
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
