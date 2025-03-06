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

package org.springframework.boot.autoconfigure.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import org.apache.commons.dbcp2.BasicDataSource;

import org.springframework.jdbc.datasource.SimpleDriverDataSource;

/**
 * {@link BasicDataSource} used for testing.
 *
 * @author Phillip Webb
 * @author Kazuki Shimizu
 * @author Stephane Nicoll
 */
public class TestDataSource extends SimpleDriverDataSource {

	/**
	 * Create an in-memory database with a random name.
	 */
	public TestDataSource() {
		this(false);
	}

	/**
	 * Create an in-memory database with a random name.
	 * @param addTestUser if a test user should be added
	 */
	public TestDataSource(boolean addTestUser) {
		this(UUID.randomUUID().toString(), addTestUser);
	}

	/**
	 * Create an in-memory database with the specified name.
	 * @param name the name of the database
	 * @param addTestUser if a test user should be added
	 */
	public TestDataSource(String name, boolean addTestUser) {
		setDriverClass(org.hsqldb.jdbc.JDBCDriver.class);
		setUrl("jdbc:hsqldb:mem:" + name);
		setUsername("sa");
		setupDatabase(addTestUser);
		setUrl(getUrl() + ";create=false");
	}

	private void setupDatabase(boolean addTestUser) {
		try (Connection connection = getConnection()) {
			if (addTestUser) {
				connection.prepareStatement("CREATE USER \"test\" password \"secret\" ADMIN").execute();
			}
		}
		catch (SQLException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
