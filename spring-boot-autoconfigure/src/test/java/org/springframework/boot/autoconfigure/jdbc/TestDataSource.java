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

package org.springframework.boot.autoconfigure.jdbc;

import org.apache.commons.dbcp2.BasicDataSource;

/**
 * {@link BasicDataSource} used for testing.
 *
 * @author Phillip Webb
 * @author Kazuki Shimizu
 */
public class TestDataSource extends BasicDataSource {

	public TestDataSource(String name) {
		setDriverClassName("org.hsqldb.jdbcDriver");
		setUrl("jdbc:hsqldb:target/" + name);
		setUsername("sa");
	}

}
