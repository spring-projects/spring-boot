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
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import liquibase.changelog.StandardChangeLogHistoryService;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.integration.spring.SpringLiquibase;

import org.springframework.boot.actuate.endpoint.LiquibaseEndpoint.LiquibaseReport;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link Endpoint} to expose liquibase info.
 *
 * @author Eddú Meléndez
 * @author Dmitrii Sergeev
 * @since 1.3.0
 */
@ConfigurationProperties(prefix = "endpoints.liquibase")
public class LiquibaseEndpoint extends AbstractEndpoint<List<LiquibaseReport>> {

	private final Map<String, SpringLiquibase> liquibases;

	public LiquibaseEndpoint(SpringLiquibase liquibase) {
		this(Collections.singletonMap("default", liquibase));
	}

	public LiquibaseEndpoint(Map<String, SpringLiquibase> liquibases) {
		super("liquibase");
		Assert.notEmpty(liquibases, "Liquibases must be specified");
		this.liquibases = liquibases;
	}

	@Override
	public List<LiquibaseReport> invoke() {
		List<LiquibaseReport> reports = new ArrayList<LiquibaseReport>();
		DatabaseFactory factory = DatabaseFactory.getInstance();
		StandardChangeLogHistoryService service = new StandardChangeLogHistoryService();
		for (Map.Entry<String, SpringLiquibase> entry : this.liquibases.entrySet()) {
			try {
				DataSource dataSource = entry.getValue().getDataSource();
				JdbcConnection connection = new JdbcConnection(
						dataSource.getConnection());
				Database database = null;
				try {
					database = factory.findCorrectDatabaseImplementation(connection);
					String defaultSchema = entry.getValue().getDefaultSchema();
					if (StringUtils.hasText(defaultSchema)) {
						database.setDefaultSchemaName(defaultSchema);
					}
					reports.add(new LiquibaseReport(entry.getKey(),
							service.queryDatabaseChangeLogTable(database)));
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
				throw new IllegalStateException("Unable to get Liquibase changelog", ex);
			}
		}

		return reports;
	}

	/**
	 * Liquibase report for one datasource.
	 */
	public static class LiquibaseReport {

		private final String name;

		private final List<Map<String, ?>> changeLogs;

		public LiquibaseReport(String name, List<Map<String, ?>> changeLogs) {
			this.name = name;
			this.changeLogs = changeLogs;
		}

		public String getName() {
			return this.name;
		}

		public List<Map<String, ?>> getChangeLogs() {
			return this.changeLogs;
		}

	}

}
