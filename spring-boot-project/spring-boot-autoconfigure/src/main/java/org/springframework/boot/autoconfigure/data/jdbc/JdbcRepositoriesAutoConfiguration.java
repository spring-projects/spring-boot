/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.jdbc;

import java.util.Optional;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.data.jdbc.repository.config.JdbcConfiguration;
import org.springframework.data.jdbc.repository.config.JdbcRepositoryConfigExtension;
import org.springframework.data.relational.core.conversion.RelationalConverter;
import org.springframework.data.relational.core.mapping.NamingStrategy;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's JDBC Repositories.
 * <p>
 * Once in effect, the auto-configuration is the equivalent of enabling JDBC repositories
 * using the {@link EnableJdbcRepositories} annotation and providing a
 * {@link JdbcConfiguration} subclass.
 *
 * @author Andy Wilkinson
 * @since 2.1.0
 * @see EnableJdbcRepositories
 */
@Configuration
@ConditionalOnBean(NamedParameterJdbcOperations.class)
@ConditionalOnClass({ NamedParameterJdbcOperations.class, JdbcConfiguration.class })
@ConditionalOnProperty(prefix = "spring.data.jdbc.repositories", name = "enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureAfter(JdbcTemplateAutoConfiguration.class)
public class JdbcRepositoriesAutoConfiguration {

	@Configuration
	@ConditionalOnMissingBean(JdbcRepositoryConfigExtension.class)
	@Import(JdbcRepositoriesAutoConfigureRegistrar.class)
	static class JdbcRepositoriesConfiguration {

	}

	@Configuration
	@ConditionalOnMissingBean(JdbcConfiguration.class)
	static class SpringBootJdbcConfiguration extends JdbcConfiguration {

		// Remove these public methods when they are made
		// public in Spring Data
		@Override
		public JdbcCustomConversions jdbcCustomConversions() {
			return super.jdbcCustomConversions();
		}

		@Override
		public RelationalMappingContext jdbcMappingContext(
				Optional<NamingStrategy> namingStrategy) {
			return super.jdbcMappingContext(namingStrategy);
		}

		@Override
		public RelationalConverter relationalConverter(
				RelationalMappingContext mappingContext) {
			return super.relationalConverter(mappingContext);
		}

	}

}
