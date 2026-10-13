/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.boot.testsupport.classpath.ClassPathExclusions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataSourceBuilder} when Hikari is not on the classpath.
 *
 * @author Phillip Webb
 */
@ClassPathExclusions("Hikari*.jar")
class DataSourceBuilderNoHikariTests {

	@Test
	void findTypeReturnsTomcatDataSource() {
		assertThat(DataSourceBuilder.findType(null)).isEqualTo(org.apache.tomcat.jdbc.pool.DataSource.class);
	}

	@Test
	void createAndBuildReturnsTomcatDataSource() {
		DataSource dataSource = DataSourceBuilder.create().build();
		assertThat(dataSource).isInstanceOf(org.apache.tomcat.jdbc.pool.DataSource.class);
	}

}
