/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.jdbc;

import java.sql.SQLException;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link SimpleDataSourceExample}.
 *
 * @author Stephane Nicoll
 */
@RunWith(SpringRunner.class)
@SpringBootTest(properties = {
		"app.datasource.url=jdbc:h2:mem:configurable;DB_CLOSE_DELAY=-1",
		"app.datasource.maximum-pool-size=42" })
@Import(ConfigurableDataSourceExample.ConfigurableDataSourceConfiguration.class)
public class ConfigurableDataSourceExampleTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void validateConfiguration() throws SQLException {
		assertThat(this.context.getBeansOfType(DataSource.class)).hasSize(1);
		HikariDataSource dataSource = this.context.getBean(HikariDataSource.class);
		assertThat(dataSource.getConnection().getMetaData().getURL())
				.isEqualTo("jdbc:h2:mem:configurable");
		assertThat(dataSource.getMaximumPoolSize()).isEqualTo(42);
	}

}
