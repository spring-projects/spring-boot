/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.dataaccess.configuretwodatasources;

import java.sql.SQLException;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MyAdditionalDataSourceConfiguration}.
 *
 * @author Stephane Nicoll
 */
@SpringBootTest(properties = { "app.datasource.jdbc-url=jdbc:h2:mem:bar;DB_CLOSE_DELAY=-1",
		"app.datasource.maximum-pool-size=42" })
@Import(MyAdditionalDataSourceConfiguration.class)
class MyDataSourcesConfigurationTests {

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
		assertThat(this.secondDataSource).extracting((dataSource) -> ((HikariDataSource) dataSource).getJdbcUrl())
			.isEqualTo("jdbc:h2:mem:bar;DB_CLOSE_DELAY=-1");
		assertThat(this.secondDataSource)
			.extracting((dataSource) -> ((HikariDataSource) dataSource).getMaximumPoolSize())
			.isEqualTo(42);
	}

}
