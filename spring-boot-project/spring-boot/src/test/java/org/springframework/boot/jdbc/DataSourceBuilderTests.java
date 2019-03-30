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

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.After;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataSourceBuilder}.
 *
 * @author Stephane Nicoll
 */
public class DataSourceBuilderTests {

	private DataSource dataSource;

	@After
	public void shutdownDataSource() throws IOException {
		if (this.dataSource instanceof Closeable) {
			((Closeable) this.dataSource).close();
		}
	}

	@Test
	public void defaultToHikari() {
		this.dataSource = DataSourceBuilder.create().url("jdbc:h2:test").build();
		assertThat(this.dataSource).isInstanceOf(HikariDataSource.class);
	}

	@Test
	public void defaultToTomcatIfHikariIsNotAvailable() {
		this.dataSource = DataSourceBuilder
				.create(new HidePackagesClassLoader("com.zaxxer.hikari"))
				.url("jdbc:h2:test").build();
		assertThat(this.dataSource)
				.isInstanceOf(org.apache.tomcat.jdbc.pool.DataSource.class);
	}

	@Test
	public void defaultToCommonsDbcp2AsLastResort() {
		this.dataSource = DataSourceBuilder
				.create(new HidePackagesClassLoader("com.zaxxer.hikari",
						"org.apache.tomcat.jdbc.pool"))
				.url("jdbc:h2:test").build();
		assertThat(this.dataSource).isInstanceOf(BasicDataSource.class);
	}

	@Test
	public void specificTypeOfDataSource() {
		HikariDataSource hikariDataSource = DataSourceBuilder.create()
				.type(HikariDataSource.class).build();
		assertThat(hikariDataSource).isInstanceOf(HikariDataSource.class);
	}

	final class HidePackagesClassLoader extends URLClassLoader {

		private final String[] hiddenPackages;

		HidePackagesClassLoader(String... hiddenPackages) {
			super(new URL[0], HidePackagesClassLoader.class.getClassLoader());
			this.hiddenPackages = hiddenPackages;
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve)
				throws ClassNotFoundException {
			if (Arrays.stream(this.hiddenPackages).anyMatch(name::startsWith)) {
				throw new ClassNotFoundException();
			}
			return super.loadClass(name, resolve);
		}

	}

}
