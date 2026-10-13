/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.autoconfigure;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.jdbc.DatabaseDriver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TomcatJdbcConnectionDetailsBeanPostProcessor}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class TomcatJdbcConnectionDetailsBeanPostProcessorTests {

	@Test
	@SuppressWarnings("unchecked")
	void setUsernamePasswordUrlAndDriverClassName() {
		DataSource dataSource = new DataSource();
		dataSource.setUrl("will-be-overwritten");
		dataSource.setUsername("will-be-overwritten");
		dataSource.setPassword("will-be-overwritten");
		dataSource.setDriverClassName("will-be-overwritten");
		new TomcatJdbcConnectionDetailsBeanPostProcessor(mock(ObjectProvider.class)).processDataSource(dataSource,
				new TestJdbcConnectionDetails());
		assertThat(dataSource.getUrl()).isEqualTo("jdbc:customdb://customdb.example.com:12345/database-1");
		assertThat(dataSource.getUsername()).isEqualTo("user-1");
		assertThat(dataSource.getPoolProperties().getPassword()).isEqualTo("password-1");
		assertThat(dataSource.getPoolProperties().getDriverClassName())
			.isEqualTo(DatabaseDriver.POSTGRESQL.getDriverClassName());
	}

}
