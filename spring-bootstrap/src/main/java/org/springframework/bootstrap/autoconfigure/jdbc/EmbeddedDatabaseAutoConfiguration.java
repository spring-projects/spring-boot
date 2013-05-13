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

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.bootstrap.context.annotation.ConditionalOnMissingBean;
import org.springframework.bootstrap.context.annotation.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
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

	private static Log logger = LogFactory
			.getLog(EmbeddedDatabaseAutoConfiguration.class);

	private static final Map<EmbeddedDatabaseType, String> EMBEDDED_DATABASE_TYPE_CLASSES;
	static {
		EMBEDDED_DATABASE_TYPE_CLASSES = new LinkedHashMap<EmbeddedDatabaseType, String>();
		EMBEDDED_DATABASE_TYPE_CLASSES.put(EmbeddedDatabaseType.HSQL,
				"org.hsqldb.Database");
	}

	// FIXME: DB platform
	@Value("${spring.jdbc.schema:classpath*:schema.sql}")
	private Resource[] schemaLocations = new Resource[0];

	@PostConstruct
	protected void initialize() throws Exception {
		ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
		boolean exists = false;
		for (Resource resource : this.schemaLocations) {
			if (resource.exists()) {
				exists = true;
				populator.addScript(resource);
				populator.setContinueOnError(true);
			}
		}
		if (exists) {
			DatabasePopulatorUtils.execute(populator, dataSource());
		}
	}

	@Bean
	public DataSource dataSource() {
		EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder()
				.setType(getEmbeddedDatabaseType());
		return builder.build();
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
				if (logger.isDebugEnabled()) {
					logger.debug("Spring JDBC not detected (EmbeddedDatabaseCondition evaluated false).");
				}
				return false;
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Spring JDBC detected (embedded database type is "
						+ getEmbeddedDatabaseType() + ").");
			}
			return getEmbeddedDatabaseType() != null;
		}
	}
}
