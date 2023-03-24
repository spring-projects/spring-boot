/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.sql.init;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.NoneNestedConditions;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration.SqlInitializationModeCondition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.sql.init.dependency.DatabaseInitializationDependencyConfigurer;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for initializing an SQL database.
 *
 * @author Andy Wilkinson
 * @since 2.5.0
 */
@AutoConfiguration
@EnableConfigurationProperties(SqlInitializationProperties.class)
@Import({ DatabaseInitializationDependencyConfigurer.class, R2dbcInitializationConfiguration.class,
		DataSourceInitializationConfiguration.class })
@ConditionalOnProperty(prefix = "spring.sql.init", name = "enabled", matchIfMissing = true)
@Conditional(SqlInitializationModeCondition.class)
public class SqlInitializationAutoConfiguration {

	static class SqlInitializationModeCondition extends NoneNestedConditions {

		SqlInitializationModeCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@ConditionalOnProperty(prefix = "spring.sql.init", name = "mode", havingValue = "never")
		static class ModeIsNever {

		}

	}

}
