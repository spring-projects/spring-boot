/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.jdbc;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.data.jdbc.repository.config.JdbcConfiguration;
import org.springframework.data.jdbc.repository.config.JdbcRepositoryConfigExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's JDBC Repositories.
 * <p>
 * Once in effect, the auto-configuration is the equivalent of enabling JDBC repositories
 * using the {@link EnableJdbcRepositories} annotation and providing an
 * {@link AbstractJdbcConfiguration} subclass.
 *
 * @author Andy Wilkinson
 * @since 2.1.0
 * @see EnableJdbcRepositories
 */
@SuppressWarnings("deprecation")
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(NamedParameterJdbcOperations.class)
@ConditionalOnClass({ NamedParameterJdbcOperations.class,
		AbstractJdbcConfiguration.class })
@ConditionalOnProperty(prefix = "spring.data.jdbc.repositories", name = "enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureAfter(JdbcTemplateAutoConfiguration.class)
public class JdbcRepositoriesAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(JdbcRepositoryConfigExtension.class)
	@Import(JdbcRepositoriesAutoConfigureRegistrar.class)
	static class JdbcRepositoriesConfiguration {

	}

	@Configuration
	@ConditionalOnMissingBean({ AbstractJdbcConfiguration.class,
			JdbcConfiguration.class })
	static class SpringBootJdbcConfiguration extends AbstractJdbcConfiguration {

	}

}
