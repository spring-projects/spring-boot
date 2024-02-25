/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.docs.data.sql.jdbctemplate;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * MyBean class.
 */
@Component
public class MyBean {

	private final JdbcTemplate jdbcTemplate;

	/**
     * Constructs a new instance of MyBean with the specified JdbcTemplate.
     *
     * @param jdbcTemplate the JdbcTemplate to be used by this MyBean
     */
    public MyBean(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
     * This method is used to perform a specific action.
     * It deletes all records from the customer table.
     */
    public void doSomething() {
		/* @chomp:line this.jdbcTemplate ... */ this.jdbcTemplate.execute("delete from customer");
	}

}
