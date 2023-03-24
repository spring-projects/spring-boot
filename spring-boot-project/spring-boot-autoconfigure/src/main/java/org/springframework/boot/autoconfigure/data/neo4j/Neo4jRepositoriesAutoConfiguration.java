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

package org.springframework.boot.autoconfigure.data.neo4j;

import org.neo4j.driver.Driver;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.ConditionalOnRepositoryType;
import org.springframework.boot.autoconfigure.data.RepositoryType;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.repository.config.Neo4jRepositoryConfigurationExtension;
import org.springframework.data.neo4j.repository.support.Neo4jRepositoryFactoryBean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's Neo4j
 * Repositories.
 * <p>
 * Activates when there is no bean of type {@link Neo4jRepositoryFactoryBean} or
 * {@link Neo4jRepositoryConfigurationExtension} configured in the context, the Spring
 * Data Neo4j {@link Neo4jRepository} type is on the classpath, the Neo4j client driver
 * API is on the classpath, and there is no other configured {@link Neo4jRepository}.
 * <p>
 * Once in effect, the auto-configuration is the equivalent of enabling Neo4j repositories
 * using the {@link EnableNeo4jRepositories @EnableNeo4jRepositories} annotation.
 *
 * @author Dave Syer
 * @author Oliver Gierke
 * @author Josh Long
 * @author Michael J. Simons
 * @since 1.4.0
 * @see EnableNeo4jRepositories
 */
@AutoConfiguration(after = Neo4jDataAutoConfiguration.class)
@ConditionalOnClass({ Driver.class, Neo4jRepository.class })
@ConditionalOnMissingBean({ Neo4jRepositoryFactoryBean.class, Neo4jRepositoryConfigurationExtension.class })
@ConditionalOnRepositoryType(store = "neo4j", type = RepositoryType.IMPERATIVE)
@Import(Neo4jRepositoriesRegistrar.class)
public class Neo4jRepositoriesAutoConfiguration {

}
