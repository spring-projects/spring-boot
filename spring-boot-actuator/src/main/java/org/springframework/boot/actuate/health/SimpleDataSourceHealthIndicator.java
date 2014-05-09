/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.actuate.health;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

/**
 * Simple implementation of {@link HealthIndicator} that returns a status and also
 * attempts a simple database test.
 * 
 * @author Dave Syer
 */
public class SimpleDataSourceHealthIndicator implements HealthIndicator<Map<String, Object>> {

	private DataSource dataSource;

	private JdbcTemplate jdbcTemplate;

	private static Map<String, String> queries = new HashMap<String, String>();

	static {
		queries.put("HSQL Database Engine",
				"SELECT COUNT(*) FROM INFORMATION_SCHEMA.SYSTEM_USERS");
		queries.put("Oracle", "SELECT 'Hello' from DUAL");
		queries.put("Apache Derby", "SELECT 1 FROM SYSIBM.SYSDUMMY1");
	}

	private static String DEFAULT_QUERY = "SELECT 1";

	private String query = null;

	/**
	 * Create a new {@link SimpleDataSourceHealthIndicator} instance.
	 */
	public SimpleDataSourceHealthIndicator() {
	}

	/**
	 * Create a new {@link SimpleDataSourceHealthIndicator} using the specified datasource.
	 * @param dataSource the data source
	 */
	public SimpleDataSourceHealthIndicator(DataSource dataSource) {
		this.dataSource = dataSource;
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Override
	public Map<String, Object> health() {
		LinkedHashMap<String, Object> health = new LinkedHashMap<String, Object>();
		health.put("status", "ok");
		String product = "unknown";
		if (this.dataSource != null) {
			try {
				product = this.jdbcTemplate.execute(new ConnectionCallback<String>() {
					@Override
					public String doInConnection(Connection connection)
							throws SQLException, DataAccessException {
						return connection.getMetaData().getDatabaseProductName();
					}
				});
				health.put("database", product);
			}
			catch (DataAccessException ex) {
				health.put("status", "error");
				health.put("error", ex.getClass().getName() + ": " + ex.getMessage());
			}
			String query = detectQuery(product);
			if (StringUtils.hasText(query)) {
				try {
					health.put("hello",
							this.jdbcTemplate.queryForObject(query, Object.class));
				}
				catch (Exception ex) {
					health.put("status", "error");
					health.put("error", ex.getClass().getName() + ": " + ex.getMessage());
				}
			}
		}
		return health;
	}

	protected String detectQuery(String product) {
		String query = this.query;
		if (!StringUtils.hasText(query)) {
			query = queries.get(product);
		}
		if (!StringUtils.hasText(query)) {
			query = DEFAULT_QUERY;
		}
		return query;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	public void setQuery(String query) {
		this.query = query;
	}

}
