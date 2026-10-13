/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.data.sql.jdbctemplate

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class MyBean(private val jdbcTemplate: JdbcTemplate) {

	fun doSomething() {
		jdbcTemplate.execute("delete from customer")
	}

}

