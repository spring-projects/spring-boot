/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.dataaccess.configuretwodatasources;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MyCompleteAdditionalDataSourceConfiguration}.
 *
 * @author Stephane Nicoll
 */
@SpringBootTest
@Import(MyCompleteAdditionalDataSourceConfiguration.class)
class MyCompleteDataSourcesConfigurationTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private DataSource dataSource;

	@Autowired
	@Qualifier("second")
	private DataSource secondDataSource;

	@Test
	void validateConfiguration() throws SQLException {
		assertThat(this.context.getBeansOfType(DataSource.class)).hasSize(2);
		assertThat(this.context.getBean("dataSource")).isSameAs(this.dataSource);
		assertThat(this.dataSource.getConnection().getMetaData().getURL()).startsWith("jdbc:h2:mem:");
		assertThat(this.context.getBean("secondDataSource")).isSameAs(this.secondDataSource);
		assertThat(this.secondDataSource.getConnection().getMetaData().getURL()).startsWith("jdbc:h2:mem:");
	}

}
