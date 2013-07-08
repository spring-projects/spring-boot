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

package org.springframework.autoconfigure.jdbc;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Test;
import org.springframework.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.autoconfigure.jdbc.EmbeddedDatabaseConfiguration;
import org.springframework.autoconfigure.jdbc.TomcatDataSourceConfiguration;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.ReflectionUtils;

import static org.junit.Assert.assertNotNull;

/**
 * Tests for {@link TomcatDataSourceConfiguration}.
 * 
 * @author Dave Syer
 */
public class TomcatDataSourceConfigurationTests {

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	private Map<Object, Object> old;

	private Map<Object, Object> map;

	@After
	public void restore() {
		if (this.map != null && this.old != null) {
			this.map.putAll(this.old);
		}
	}

	@Test
	public void testDataSourceExists() throws Exception {
		this.context.register(TomcatDataSourceConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(DataSource.class));
	}

	@Test(expected = BeanCreationException.class)
	public void testBadUrl() throws Exception {
		this.map = getField(EmbeddedDatabaseConfiguration.class, "EMBEDDED_DATABASE_URLS");
		this.old = new HashMap<Object, Object>(this.map);
		this.map.clear();
		this.context.register(TomcatDataSourceConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(DataSource.class));
	}

	@Test(expected = BeanCreationException.class)
	public void testBadDriverClass() throws Exception {
		this.map = getField(EmbeddedDatabaseConfiguration.class,
				"EMBEDDED_DATABASE_DRIVER_CLASSES");
		this.old = new HashMap<Object, Object>(this.map);
		this.map.clear();
		this.context.register(TomcatDataSourceConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(DataSource.class));
	}

	@SuppressWarnings("unchecked")
	public static <T> T getField(Class<?> target, String name) {
		Field field = ReflectionUtils.findField(target, name, null);
		ReflectionUtils.makeAccessible(field);
		return (T) ReflectionUtils.getField(field, target);
	}
}
