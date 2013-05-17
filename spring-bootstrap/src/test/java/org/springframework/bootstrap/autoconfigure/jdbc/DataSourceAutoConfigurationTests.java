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
package org.springframework.bootstrap.autoconfigure.jdbc;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.Test;
import org.springframework.bootstrap.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.util.ClassUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Dave Syer
 * 
 */
public class DataSourceAutoConfigurationTests {

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Test
	public void testDefaultDataSourceExists() throws Exception {
		this.context.register(DataSourceAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(DataSource.class));
	}

	@Test
	public void testJdbcTemplateExists() throws Exception {
		this.context.register(DataSourceAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		JdbcTemplate jdbcTemplate = this.context.getBean(JdbcTemplate.class);
		assertNotNull(jdbcTemplate);
		assertNotNull(jdbcTemplate.getDataSource());
	}

	@Test
	public void testNamedParameterJdbcTemplateExists() throws Exception {
		this.context.register(DataSourceAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(NamedParameterJdbcOperations.class));
	}

	@Test
	public void testDataSourceInitialized() throws Exception {
		this.context.register(DataSourceAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("spring.database.schema",
				ClassUtils.addResourcePathToPackagePath(getClass(), "schema.sql"));
		this.context.getEnvironment().getPropertySources()
				.addFirst(new MapPropertySource("test", map));
		this.context.refresh();
		DataSource dataSource = this.context.getBean(DataSource.class);
		assertTrue(dataSource instanceof org.apache.tomcat.jdbc.pool.DataSource);
		assertNotNull(dataSource);
		JdbcOperations template = new JdbcTemplate(dataSource);
		assertEquals(new Integer(0),
				template.queryForObject("SELECT COUNT(*) from FOO", Integer.class));
	}

}
