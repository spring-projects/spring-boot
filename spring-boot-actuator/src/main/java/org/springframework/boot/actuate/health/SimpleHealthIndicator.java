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

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

/**
 * @author Dave Syer
 */
public class SimpleHealthIndicator implements HealthIndicator<Map<String, Object>>,
		EnvironmentAware {

	private Environment environment;

	private DataSource dataSource;

	private JdbcTemplate jdbcTemplate;

	private String query = "SELECT 'Hello'";

	@Override
	public Map<String, Object> health() {
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		map.put("status", "ok");
		if (this.dataSource != null) {
			try {
				map.put("database", this.dataSource.getConnection().getMetaData()
						.getDatabaseProductName());
			}
			catch (SQLException ex) {
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
		if (this.environment != null) {
			map.put("spring.profiles.active", StringUtils
					.arrayToCommaDelimitedString(this.environment.getActiveProfiles()));
		}
		return map;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	public void setQuery(String query) {
		this.query = query;
	}

}
