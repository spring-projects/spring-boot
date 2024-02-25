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

package org.springframework.boot.actuate.liquibase;

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
 * @since 2.0.0
 */
@Endpoint(id = "liquibase")
public class LiquibaseEndpoint {

	private final ApplicationContext context;

	/**
     * Constructs a new LiquibaseEndpoint with the specified ApplicationContext.
     * 
     * @param context the ApplicationContext to be used by the LiquibaseEndpoint
     * @throws IllegalArgumentException if the context is null
     */
    public LiquibaseEndpoint(ApplicationContext context) {
		Assert.notNull(context, "Context must be specified");
		this.context = context;
	}

	/**
     * Retrieves the Liquibase beans descriptor for the current application context.
     * 
     * @return The Liquibase beans descriptor containing information about the Liquibase beans in the application context.
     */
    @ReadOperation
	public LiquibaseBeansDescriptor liquibaseBeans() {
		ApplicationContext target = this.context;
		Map<String, ContextLiquibaseBeansDescriptor> contextBeans = new HashMap<>();
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

	/**
     * Creates a LiquibaseBeanDescriptor by executing Liquibase change sets.
     * 
     * @param liquibase The SpringLiquibase instance.
     * @param factory The DatabaseFactory instance.
     * @return The LiquibaseBeanDescriptor containing the executed change sets.
     * @throws IllegalStateException if unable to get Liquibase change sets.
     */
    private LiquibaseBeanDescriptor createReport(SpringLiquibase liquibase, DatabaseFactory factory) {
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

		private final Map<String, ContextLiquibaseBeansDescriptor> contexts;

		/**
         * Constructs a new LiquibaseBeansDescriptor with the specified contexts.
         *
         * @param contexts the map of contexts to be associated with this LiquibaseBeansDescriptor
         */
        private LiquibaseBeansDescriptor(Map<String, ContextLiquibaseBeansDescriptor> contexts) {
			this.contexts = contexts;
		}

		/**
         * Returns the map of contexts associated with this LiquibaseBeansDescriptor.
         * 
         * @return the map of contexts
         */
        public Map<String, ContextLiquibaseBeansDescriptor> getContexts() {
			return this.contexts;
		}

	}

	/**
	 * Description of an application context's {@link SpringLiquibase} beans.
	 */
	public static final class ContextLiquibaseBeansDescriptor {

		private final Map<String, LiquibaseBeanDescriptor> liquibaseBeans;

		private final String parentId;

		/**
         * Constructs a new ContextLiquibaseBeansDescriptor with the specified liquibaseBeans and parentId.
         * 
         * @param liquibaseBeans the map of liquibase beans
         * @param parentId the parent ID
         */
        private ContextLiquibaseBeansDescriptor(Map<String, LiquibaseBeanDescriptor> liquibaseBeans, String parentId) {
			this.liquibaseBeans = liquibaseBeans;
			this.parentId = parentId;
		}

		/**
         * Returns the map of Liquibase bean descriptors.
         *
         * @return the map of Liquibase bean descriptors
         */
        public Map<String, LiquibaseBeanDescriptor> getLiquibaseBeans() {
			return this.liquibaseBeans;
		}

		/**
         * Returns the parent ID of the ContextLiquibaseBeansDescriptor.
         * 
         * @return the parent ID of the ContextLiquibaseBeansDescriptor
         */
        public String getParentId() {
			return this.parentId;
		}

	}

	/**
	 * Description of a {@link SpringLiquibase} bean.
	 */
	public static final class LiquibaseBeanDescriptor {

		private final List<ChangeSetDescriptor> changeSets;

		/**
         * Constructs a new LiquibaseBeanDescriptor with the specified list of ChangeSetDescriptors.
         * 
         * @param changeSets the list of ChangeSetDescriptors to be associated with this LiquibaseBeanDescriptor
         */
        public LiquibaseBeanDescriptor(List<ChangeSetDescriptor> changeSets) {
			this.changeSets = changeSets;
		}

		/**
         * Returns the list of ChangeSetDescriptors.
         *
         * @return the list of ChangeSetDescriptors
         */
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

		private final String checksum;

		private final Integer orderExecuted;

		private final String tag;

		/**
         * Constructs a ChangeSetDescriptor object based on the provided RanChangeSet object.
         * 
         * @param ranChangeSet the RanChangeSet object to create the ChangeSetDescriptor from
         */
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

		/**
         * Returns the author of the ChangeSetDescriptor.
         *
         * @return the author of the ChangeSetDescriptor
         */
        public String getAuthor() {
			return this.author;
		}

		/**
         * Returns the change log of the ChangeSetDescriptor.
         * 
         * @return the change log of the ChangeSetDescriptor
         */
        public String getChangeLog() {
			return this.changeLog;
		}

		/**
         * Returns the comments associated with this ChangeSetDescriptor.
         * 
         * @return the comments associated with this ChangeSetDescriptor
         */
        public String getComments() {
			return this.comments;
		}

		/**
         * Returns the set of contexts associated with this ChangeSetDescriptor.
         *
         * @return the set of contexts
         */
        public Set<String> getContexts() {
			return this.contexts;
		}

		/**
         * Returns the date and time when the change set was executed.
         *
         * @return the date and time when the change set was executed
         */
        public Instant getDateExecuted() {
			return this.dateExecuted;
		}

		/**
         * Returns the deployment ID of the ChangeSetDescriptor.
         * 
         * @return the deployment ID of the ChangeSetDescriptor
         */
        public String getDeploymentId() {
			return this.deploymentId;
		}

		/**
         * Returns the description of the ChangeSetDescriptor.
         *
         * @return the description of the ChangeSetDescriptor
         */
        public String getDescription() {
			return this.description;
		}

		/**
         * Returns the execution type of the ChangeSetDescriptor.
         * 
         * @return the execution type of the ChangeSetDescriptor
         */
        public ExecType getExecType() {
			return this.execType;
		}

		/**
         * Returns the ID of the ChangeSetDescriptor.
         *
         * @return the ID of the ChangeSetDescriptor
         */
        public String getId() {
			return this.id;
		}

		/**
         * Returns the set of labels associated with this ChangeSetDescriptor.
         *
         * @return the set of labels
         */
        public Set<String> getLabels() {
			return this.labels;
		}

		/**
         * Returns the checksum of the ChangeSetDescriptor.
         *
         * @return the checksum of the ChangeSetDescriptor
         */
        public String getChecksum() {
			return this.checksum;
		}

		/**
         * Returns the number of orders executed.
         *
         * @return the number of orders executed
         */
        public Integer getOrderExecuted() {
			return this.orderExecuted;
		}

		/**
         * Returns the tag of the ChangeSetDescriptor.
         *
         * @return the tag of the ChangeSetDescriptor
         */
        public String getTag() {
			return this.tag;
		}

	}

	/**
	 * Description of a context expression in a {@link ChangeSetDescriptor}.
	 */
	public static class ContextExpressionDescriptor {

		private final Set<String> contexts;

		/**
         * Constructs a new ContextExpressionDescriptor with the specified set of contexts.
         * 
         * @param contexts the set of contexts to be associated with the descriptor
         */
        public ContextExpressionDescriptor(Set<String> contexts) {
			this.contexts = contexts;
		}

		/**
         * Returns the set of contexts associated with the ContextExpressionDescriptor.
         *
         * @return the set of contexts
         */
        public Set<String> getContexts() {
			return this.contexts;
		}

	}

}
