/*
 * Copyright 2012-2021 the original author or authors.
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

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MyDataSourcesConfiguration}.
 *
 * @author Stephane Nicoll
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(properties = { "app.datasource.second.url=jdbc:h2:mem:bar;DB_CLOSE_DELAY=-1",
		"app.datasource.second.max-total=42" })
@Import(MyDataSourcesConfiguration.class)
class MyDataSourcesConfigurationTests {

	@Autowired
	private ApplicationContext context;

	@Test
	void validateConfiguration() throws SQLException {
		assertThat(this.context.getBeansOfType(DataSource.class)).hasSize(2);
		DataSource dataSource = this.context.getBean(DataSource.class);
		assertThat(this.context.getBean("firstDataSource")).isSameAs(dataSource);
		assertThat(dataSource.getConnection().getMetaData().getURL()).startsWith("jdbc:h2:mem:");
		BasicDataSource secondDataSource = this.context.getBean("secondDataSource", BasicDataSource.class);
		assertThat(secondDataSource.getUrl()).isEqualTo("jdbc:h2:mem:bar;DB_CLOSE_DELAY=-1");
		assertThat(secondDataSource.getMaxTotal()).isEqualTo(42);
	}

}
