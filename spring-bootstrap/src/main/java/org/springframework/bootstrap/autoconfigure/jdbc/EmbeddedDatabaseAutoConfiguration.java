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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.bootstrap.context.annotation.ConditionalOnMissingBean;
import org.springframework.bootstrap.context.annotation.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.task.AsyncUtils;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.util.ClassUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for embedded databases.
 * 
 * @author Phillip Webb
 */
@Configuration
@Conditional(EmbeddedDatabaseAutoConfiguration.EmbeddedDatabaseCondition.class)
@ConditionalOnMissingBean(DataSource.class)
public class EmbeddedDatabaseAutoConfiguration {

	private static final Map<EmbeddedDatabaseType, String> EMBEDDED_DATABASE_TYPE_CLASSES;
	static {
		EMBEDDED_DATABASE_TYPE_CLASSES = new LinkedHashMap<EmbeddedDatabaseType, String>();
		EMBEDDED_DATABASE_TYPE_CLASSES.put(EmbeddedDatabaseType.HSQL,
				"org.hsqldb.Database");
	}

	private ExecutorService executorService;

	@Autowired
	private ResourceLoader resourceLoader = new DefaultResourceLoader();

	@Value("${spring.jdbc.schema:classpath:schema.sql}")
	private String schemaLocation = "classpath:schema.sql"; // FIXME: DB platform

	@PostConstruct
	protected void initialize() throws Exception {
		Resource resource = this.resourceLoader.getResource(this.schemaLocation);
		if (resource.exists()) {
			ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
			populator.addScript(resource);
			populator.setContinueOnError(true);
			DatabasePopulatorUtils.execute(populator, dataSource());
		}
	}

	@Bean
	public DataSource dataSource() {
		EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder()
				.setType(getEmbeddedDatabaseType());
		return AsyncUtils.submitVia(this.executorService, builder).build();
	}

	public static EmbeddedDatabaseType getEmbeddedDatabaseType() {
		for (Map.Entry<EmbeddedDatabaseType, String> entry : EMBEDDED_DATABASE_TYPE_CLASSES
				.entrySet()) {
			if (ClassUtils.isPresent(entry.getValue(),
					EmbeddedDatabaseAutoConfiguration.class.getClassLoader())) {
				return entry.getKey();
			}
		}
		return null;
	}

	static class EmbeddedDatabaseCondition implements Condition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			if (!ClassUtils.isPresent(
					"org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType",
					context.getClassLoader())) {
				return false;
			}
			return getEmbeddedDatabaseType() != null;
		}
	}
}
