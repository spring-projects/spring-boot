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

package org.springframework.boot.autoconfigure.jdbc;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.PreDestroy;
import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.util.ClassUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for embedded databases.
 * 
 * @author Phillip Webb
 */
@Configuration
public class EmbeddedDatabaseConfiguration {

	private static final Map<EmbeddedDatabaseType, String> EMBEDDED_DATABASE_DRIVER_CLASSES;
	static {
		Map<EmbeddedDatabaseType, String> drivers = new LinkedHashMap<EmbeddedDatabaseType, String>();
		drivers.put(EmbeddedDatabaseType.H2, "org.h2.Driver");
		drivers.put(EmbeddedDatabaseType.DERBY, "org.apache.derby.jdbc.EmbeddedDriver");
		drivers.put(EmbeddedDatabaseType.HSQL, "org.hsqldb.jdbcDriver");
		EMBEDDED_DATABASE_DRIVER_CLASSES = drivers;
	}

	private static final Map<EmbeddedDatabaseType, String> EMBEDDED_DATABASE_URLS;
	static {
		Map<EmbeddedDatabaseType, String> urls = new LinkedHashMap<EmbeddedDatabaseType, String>();
		urls.put(EmbeddedDatabaseType.H2, "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
		urls.put(EmbeddedDatabaseType.DERBY, "jdbc:derby:memory:testdb;create=true");
		urls.put(EmbeddedDatabaseType.HSQL, "jdbc:hsqldb:mem:testdb");
		EMBEDDED_DATABASE_URLS = urls;
	}

	private EmbeddedDatabase database;

	@Bean
	public DataSource dataSource() {
		EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder()
				.setType(getEmbeddedDatabaseType());
		this.database = builder.build();
		return this.database;
	}

	@PreDestroy
	public void close() {
		if (this.database != null) {
			this.database.shutdown();
		}
	}

	public static String getEmbeddedDatabaseDriverClass(
			EmbeddedDatabaseType embeddedDatabaseType) {
		return EMBEDDED_DATABASE_DRIVER_CLASSES.get(embeddedDatabaseType);
	}

	public static String getEmbeddedDatabaseUrl(EmbeddedDatabaseType embeddedDatabaseType) {
		return EMBEDDED_DATABASE_URLS.get(embeddedDatabaseType);
	}

	public static EmbeddedDatabaseType getEmbeddedDatabaseType() {
		for (Map.Entry<EmbeddedDatabaseType, String> entry : EMBEDDED_DATABASE_DRIVER_CLASSES
				.entrySet()) {
			if (ClassUtils.isPresent(entry.getValue(),
					EmbeddedDatabaseConfiguration.class.getClassLoader())) {
				return entry.getKey();
			}
		}
		return null;
	}

}
