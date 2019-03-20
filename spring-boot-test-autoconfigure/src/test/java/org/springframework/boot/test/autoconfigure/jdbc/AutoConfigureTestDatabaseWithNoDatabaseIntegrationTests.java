/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.jdbc;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link AutoConfigureTestDatabase} when there is no database.
 *
 * @author Phillip Webb
 */
@RunWith(SpringRunner.class)
@AutoConfigureTestDatabase
public class AutoConfigureTestDatabaseWithNoDatabaseIntegrationTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void testContextLoads() throws Exception {
		// gh-6897
		assertThat(this.context).isNotNull();
		assertThat(this.context.getBeanNamesForType(DataSource.class)).isNotEmpty();
	}

	@TestConfiguration
	static class Config {

	}

}
