/*
 * Copyright 2012-2013 the original author or authors.
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
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

/**
 * @author Dave Syer
 */
public class SimpleHealthIndicator implements HealthIndicator<Map<String, Object>> {

	private DataSource dataSource;

	private JdbcTemplate jdbcTemplate;

	private String query = "SELECT 'Hello'";

	@Override
	public Map<String, Object> health() {
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		map.put("status", "ok");
		if (this.dataSource != null) {
			try {
				String product = this.jdbcTemplate
						.execute(new ConnectionCallback<String>() {
							@Override
							public String doInConnection(Connection connection)
									throws SQLException, DataAccessException {
								return connection.getMetaData().getDatabaseProductName();
							}
						});
				map.put("database", product);
			}
			catch (DataAccessException ex) {
				map.put("status", "error");
				map.put("error", ex.getClass().getName() + ": " + ex.getMessage());
			}
			if (StringUtils.hasText(this.query)) {
				try {
					map.put("hello",
							this.jdbcTemplate.queryForObject(this.query, String.class));
				}
				catch (Exception ex) {
					map.put("status", "error");
					map.put("error", ex.getClass().getName() + ": " + ex.getMessage());
				}
			}
		}
		return map;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	public void setQuery(String query) {
		this.query = query;
	}

}
