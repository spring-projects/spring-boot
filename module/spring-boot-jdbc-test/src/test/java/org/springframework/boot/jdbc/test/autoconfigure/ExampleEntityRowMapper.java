/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.test.autoconfigure;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

/**
 * @author Stephane Nicoll
 */
class ExampleEntityRowMapper implements RowMapper<ExampleEntity> {

	@Override
	public ExampleEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
		int id = rs.getInt("id");
		String name = rs.getString("name");
		return new ExampleEntity(id, name);
	}

}
