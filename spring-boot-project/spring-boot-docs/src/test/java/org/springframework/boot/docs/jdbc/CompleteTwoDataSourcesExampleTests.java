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

package org.springframework.boot.docs.jdbc;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CompleteTwoDataSourcesExample}.
 *
 * @author Stephane Nicoll
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@Import(CompleteTwoDataSourcesExample.CompleteDataSourcesConfiguration.class)
public class CompleteTwoDataSourcesExampleTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void validateConfiguration() throws SQLException {
		assertThat(this.context.getBeansOfType(DataSource.class)).hasSize(2);
		DataSource dataSource = this.context.getBean(DataSource.class);
		assertThat(this.context.getBean("firstDataSource")).isSameAs(dataSource);
		assertThat(dataSource.getConnection().getMetaData().getURL()).startsWith("jdbc:h2:mem:");
		DataSource secondDataSource = this.context.getBean("secondDataSource", DataSource.class);
		assertThat(secondDataSource.getConnection().getMetaData().getURL()).startsWith("jdbc:h2:mem:");
	}

}
