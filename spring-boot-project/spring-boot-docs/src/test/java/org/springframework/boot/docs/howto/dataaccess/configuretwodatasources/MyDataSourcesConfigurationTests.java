/*
 * Copyright 2012-2024 the original author or authors.
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
