/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.jdbc;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EmbeddedDatabaseConnection}.
 *
 * @author Stephane Nicoll
 */
public class EmbeddedDatabaseConnectionTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Test
	public void h2CustomDatabaseName() {
		assertThat(EmbeddedDatabaseConnection.H2.getUrl("mydb"))
				.isEqualTo("jdbc:h2:mem:mydb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
	}

	@Test
	public void derbyCustomDatabaseName() {
		assertThat(EmbeddedDatabaseConnection.DERBY.getUrl("myderbydb"))
				.isEqualTo("jdbc:derby:memory:myderbydb;create=true");
	}

	@Test
	public void hsqlCustomDatabaseName() {
		assertThat(EmbeddedDatabaseConnection.HSQL.getUrl("myhsql"))
				.isEqualTo("jdbc:hsqldb:mem:myhsql");
	}

	@Test
	public void getUrlWithNoDatabaseName() {
		this.thrown.expect(IllegalArgumentException.class);
		EmbeddedDatabaseConnection.H2.getUrl("  ");
	}

}
