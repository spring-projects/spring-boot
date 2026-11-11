/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.jpa.test.autoconfigure;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link DataJpaTest @DataJpaTest} with schema credentials that
 * should be ignored to allow the auto-configured test database to be used.
 *
 * @author Andy Wilkinson
 */
@DataJpaTest(properties = { "spring.sql.init.username=alice", "spring.sql.init.password=secret",
		"spring.sql.init.schema-locations=classpath:org/springframework/boot/data/jpa/test/autoconfigure/schema.sql" })
class DataJpaTestSchemaCredentialsIntegrationTests {

	@Autowired
	private DataSource dataSource;

	@Test
	void replacesDefinedDataSourceWithEmbeddedDefault() throws Exception {
		String product = this.dataSource.getConnection().getMetaData().getDatabaseProductName();
		assertThat(product).isEqualTo("H2");
		assertThat(new JdbcTemplate(this.dataSource).queryForList("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES",
				String.class))
			.contains("EXAMPLE");
	}

}
