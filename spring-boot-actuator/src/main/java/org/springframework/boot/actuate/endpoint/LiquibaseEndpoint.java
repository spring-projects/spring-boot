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

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import liquibase.changelog.StandardChangeLogHistoryService;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.integration.spring.SpringLiquibase;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

/**
 * {@link Endpoint} to expose liquibase info.
 *
 * @author Eddú Meléndez
 * @since 1.3.0
 */
@ConfigurationProperties(prefix = "endpoints.liquibase", ignoreUnknownFields = true)
public class LiquibaseEndpoint extends AbstractEndpoint<List<Map<String, ?>>> {

	private final SpringLiquibase liquibase;

	public LiquibaseEndpoint(SpringLiquibase liquibase) {
		super("liquibase");
		Assert.notNull(liquibase, "Liquibase must not be null");
		this.liquibase = liquibase;
	}

	@Override
	public List<Map<String, ?>> invoke() {
		StandardChangeLogHistoryService service = new StandardChangeLogHistoryService();
		try {
			DatabaseFactory factory = DatabaseFactory.getInstance();
			DataSource dataSource = this.liquibase.getDataSource();
			JdbcConnection connection = new JdbcConnection(dataSource.getConnection());
			Database database = factory.findCorrectDatabaseImplementation(connection);
			return service.queryDatabaseChangeLogTable(database);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to get Liquibase changelog", ex);
		}
	}

}
