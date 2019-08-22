/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.devtools.autoconfigure;

import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.context.ConfigurableApplicationContext;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
		verify(statement, never()).execute("SHUTDOWN");
	}

}
