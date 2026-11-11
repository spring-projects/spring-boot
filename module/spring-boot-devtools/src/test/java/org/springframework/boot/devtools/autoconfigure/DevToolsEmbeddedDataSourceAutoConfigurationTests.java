/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.autoconfigure;

import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.context.ConfigurableApplicationContext;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * Tests for {@link DevToolsDataSourceAutoConfiguration} with an embedded data source.
 *
 * @author Andy Wilkinson
 */
@ClassPathExclusions("HikariCP-*.jar")
class DevToolsEmbeddedDataSourceAutoConfigurationTests extends AbstractDevToolsDataSourceAutoConfigurationTests {

	@Test
	void autoConfiguredDataSourceIsNotShutdown() throws SQLException {
		ConfigurableApplicationContext context = createContext(DataSourceAutoConfiguration.class,
				DataSourceSpyConfiguration.class);
		Statement statement = configureDataSourceBehavior(context.getBean(DataSource.class));
		context.close();
		then(statement).should(never()).execute("SHUTDOWN");
	}

}
