/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.autoconfigure;

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
