/*
 * Copyright 2023-2023 the original author or authors.
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

import java.util.Collection;

import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Example repository used with {@link JdbcClient JdbcClient} and
 * {@link JdbcTest @JdbcTest} tests.
 *
 * @author Yanming Zhou
 */
class ExampleJdbcClientRepository {

	private static final ExampleEntityRowMapper ROW_MAPPER = new ExampleEntityRowMapper();

	private final JdbcClient jdbcClient;

	ExampleJdbcClientRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	void save(ExampleEntity entity) {
		this.jdbcClient.sql("insert into example (id, name) values (:id, :name)")
			.param("id", entity.getId())
			.param("name", entity.getName())
			.update();
	}

	ExampleEntity findById(int id) {
		return this.jdbcClient.sql("select id, name from example where id = :id")
			.param("id", id)
			.query(ROW_MAPPER)
			.single();
	}

	Collection<ExampleEntity> findAll() {
		return this.jdbcClient.sql("select id, name from example").query(ROW_MAPPER).list();
	}

}
