/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.neo4j.autoconfigure.observation;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.neo4j.driver.Config.ConfigBuilder;
import org.neo4j.driver.observation.micrometer.MicrometerObservationProvider;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.neo4j.autoconfigure.ConfigBuilderCustomizer;
import org.springframework.boot.neo4j.autoconfigure.Neo4jAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;

/**
 * Auto-configuration for Neo4j observability.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@AutoConfiguration(before = Neo4jAutoConfiguration.class,
		afterName = "org.springframework.boot.micrometer.observation.autoconfigure.ObservationAutoConfiguration")
@ConditionalOnBean(ObservationRegistry.class)
@ConditionalOnClass({ ConfigBuilder.class, MicrometerObservationProvider.class, Observation.class })
public final class Neo4jObservationAutoConfiguration {

	@Bean
	@ConditionalOnBean(ObservationRegistry.class)
	@Order(0)
	ConfigBuilderCustomizer neo4jObservationCustomizer(ObservationRegistry registry) {
		return (builder) -> builder.withObservationProvider(MicrometerObservationProvider.builder(registry).build());
	}

}
