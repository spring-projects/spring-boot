/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.test.autoconfigure;

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
