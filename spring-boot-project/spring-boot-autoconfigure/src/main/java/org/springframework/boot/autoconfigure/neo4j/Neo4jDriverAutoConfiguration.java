/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.neo4j;

import org.neo4j.driver.Driver;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.data.neo4j.Neo4jDataAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Automatic configuration of Neo4js Java Driver.
 * <p>
 * Provides an instance of {@link Driver} if the required library is available and no
 * other instance has been manually configured.
 *
 * @author Michael J. Simons
 * @since 2.4.0
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore(Neo4jDataAutoConfiguration.class)
@ConditionalOnClass(Driver.class)
@EnableConfigurationProperties(Neo4jDriverProperties.class)
@Import(DriverConfiguration.class)
public class Neo4jDriverAutoConfiguration {

}
