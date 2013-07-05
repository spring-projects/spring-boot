/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.zero.autoconfigure.jdbc;

import javax.sql.DataSource;

import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.zero.autoconfigure.jdbc.EmbeddedDatabaseConfiguration;

import static org.junit.Assert.assertNotNull;

/**
 * @author Dave Syer
 */
public class EmbeddedDatabaseConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@Test
	public void testDefaultEmbeddedDatabase() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(EmbeddedDatabaseConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(DataSource.class));
		this.context.close();
	}

}
