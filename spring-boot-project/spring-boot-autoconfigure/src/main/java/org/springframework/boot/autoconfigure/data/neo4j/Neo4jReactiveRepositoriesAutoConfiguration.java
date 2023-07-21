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
import reactor.core.publisher.Flux;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.ConditionalOnRepositoryType;
import org.springframework.boot.autoconfigure.data.RepositoryType;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.neo4j.repository.config.ReactiveNeo4jRepositoryConfigurationExtension;
import org.springframework.data.neo4j.repository.support.ReactiveNeo4jRepositoryFactoryBean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's Neo4j Reactive
 * Repositories.
 *
 * @author Michael J. Simons
 * @author Stephane Nicoll
 * @since 2.4.0
 */
@AutoConfiguration(after = Neo4jReactiveDataAutoConfiguration.class)
@ConditionalOnClass({ Driver.class, ReactiveNeo4jRepository.class, Flux.class })
@ConditionalOnMissingBean({ ReactiveNeo4jRepositoryFactoryBean.class,
		ReactiveNeo4jRepositoryConfigurationExtension.class })
@ConditionalOnRepositoryType(store = "neo4j", type = RepositoryType.REACTIVE)
@Import(Neo4jReactiveRepositoriesRegistrar.class)
public class Neo4jReactiveRepositoriesAutoConfiguration {

}
