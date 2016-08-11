/*
 * Copyright 2012-2016 the original author or authors.
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

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import liquibase.changelog.StandardChangeLogHistoryService;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.integration.spring.SpringLiquibase;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

/**
 * {@link Endpoint} to expose liquibase info.
 *
 * @author Eddú Meléndez
 * @since 1.3.0
 */
@ConfigurationProperties(prefix = "endpoints.liquibase")
public class LiquibaseEndpoint extends AbstractEndpoint<Map<String, List<Map<String, ?>>>> {

	private final List<SpringLiquibase> liquibase;

	public LiquibaseEndpoint(SpringLiquibase liquibase) {
		this(Collections.singletonList(liquibase));
	}

	public LiquibaseEndpoint(List<SpringLiquibase> liquibase) {
		super("liquibase");
		Assert.notNull(liquibase, "Liquibase must not be null");
		this.liquibase = liquibase;
	}

	@Override
	public Map<String, List<Map<String, ?>>> invoke() {
		Map<String, List<Map<String, ?>>> services = new HashMap<String, List<Map<String, ?>>>();

		DatabaseFactory factory = DatabaseFactory.getInstance();

		for (SpringLiquibase liquibase : this.liquibase) {
			StandardChangeLogHistoryService service = new StandardChangeLogHistoryService();
			try {
				DatabaseMetaData metaData = liquibase.getDataSource().getConnection().getMetaData();
				try {
					DataSource dataSource = liquibase.getDataSource();
					JdbcConnection connection = new JdbcConnection(dataSource.getConnection());
					try {
						Database database = factory.findCorrectDatabaseImplementation(connection);
						services.put(metaData.getURL(), service.queryDatabaseChangeLogTable(database));
					}
					finally {
						connection.close();
					}
				}
				catch (DatabaseException ex) {
					throw new IllegalStateException("Unable to get Liquibase changelog", ex);
				}
			}
			catch (SQLException e) {
				//Continue
			}
		}
		return services;
	}

}
