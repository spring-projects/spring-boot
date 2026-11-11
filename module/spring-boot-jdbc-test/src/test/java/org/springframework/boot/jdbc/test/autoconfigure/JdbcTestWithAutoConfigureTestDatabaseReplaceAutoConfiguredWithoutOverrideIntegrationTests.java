/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.test.autoconfigure;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link JdbcTest @JdbcTest}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
@JdbcTest
@AutoConfigureTestDatabase(replace = Replace.AUTO_CONFIGURED)
class JdbcTestWithAutoConfigureTestDatabaseReplaceAutoConfiguredWithoutOverrideIntegrationTests {

	@Autowired
	private DataSource dataSource;

	@Test
	void usesDefaultEmbeddedDatabase() throws Exception {
		String product = this.dataSource.getConnection().getMetaData().getDatabaseProductName();
		// @AutoConfigureTestDatabase would use H2 but HSQL is manually defined
		assertThat(product).startsWith("HSQL");
	}

}
