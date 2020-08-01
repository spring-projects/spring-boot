/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

import javax.transaction.Transactional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * Example repository used with {@link JdbcTest @JdbcTest} tests.
 *
 * @author Stephane Nicoll
 */
@Repository
public class ExampleRepository {

	private static final ExampleEntityRowMapper ROW_MAPPER = new ExampleEntityRowMapper();

	private final JdbcTemplate jdbcTemplate;

	public ExampleRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Transactional
	public void save(ExampleEntity entity) {
		this.jdbcTemplate.update("insert into example (id, name) values (?, ?)", entity.getId(), entity.getName());
	}

	public ExampleEntity findById(int id) {
		return this.jdbcTemplate.queryForObject("select id, name from example where id =?", ROW_MAPPER, id);
	}

	public Collection<ExampleEntity> findAll() {
		return this.jdbcTemplate.query("select id, name from example", ROW_MAPPER);
	}

	static class ExampleEntityRowMapper implements RowMapper<ExampleEntity> {

		@Override
		public ExampleEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
			int id = rs.getInt("id");
			String name = rs.getString("name");
			return new ExampleEntity(id, name);
		}

	}

}
