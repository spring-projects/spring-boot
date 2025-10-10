/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.jdbc.test.autoconfigure;

import java.util.Collection;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Example repository used with {@link JdbcTest @JdbcTest} tests.
 *
 * @author Stephane Nicoll
 */
@Repository
class ExampleRepository {

	private static final ExampleEntityRowMapper ROW_MAPPER = new ExampleEntityRowMapper();

	private final JdbcTemplate jdbcTemplate;

	ExampleRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Transactional
	void save(ExampleEntity entity) {
		this.jdbcTemplate.update("insert into example (id, name) values (?, ?)", entity.getId(), entity.getName());
	}

	ExampleEntity findById(int id) {
		return this.jdbcTemplate.queryForObject("select id, name from example where id =?", ROW_MAPPER, id);
	}

	Collection<ExampleEntity> findAll() {
		return this.jdbcTemplate.query("select id, name from example", ROW_MAPPER);
	}

}
