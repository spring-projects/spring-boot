/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.data.sql.jdbctemplate;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class MyBean {

	private final JdbcTemplate jdbcTemplate;

	public MyBean(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void doSomething() {
		/* @chomp:line this.jdbcTemplate ... */ this.jdbcTemplate.execute("delete from customer");
	}

}
