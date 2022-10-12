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

package org.springframework.boot.actuate.autoconfigure.metrics.graphql;

import graphql.GraphQL;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties;
import org.springframework.boot.actuate.autoconfigure.metrics.PropertiesAutoTimer;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.metrics.graphql.DefaultGraphQlTagsProvider;
import org.springframework.boot.actuate.metrics.graphql.GraphQlMetricsInstrumentation;
import org.springframework.boot.actuate.metrics.graphql.GraphQlTagsContributor;
import org.springframework.boot.actuate.metrics.graphql.GraphQlTagsProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.execution.GraphQlSource;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for instrumentation of Spring
 * GraphQL endpoints.
 *
 * @author Brian Clozel
 * @since 2.7.0
 */
@AutoConfiguration(after = { MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class,
		SimpleMetricsExportAutoConfiguration.class })
@ConditionalOnBean(MeterRegistry.class)
@ConditionalOnClass({ GraphQL.class, GraphQlSource.class })
@EnableConfigurationProperties(MetricsProperties.class)
public class GraphQlMetricsAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(GraphQlTagsProvider.class)
	public DefaultGraphQlTagsProvider graphQlTagsProvider(ObjectProvider<GraphQlTagsContributor> contributors) {
		return new DefaultGraphQlTagsProvider(contributors.orderedStream().toList());
	}

	@Bean
	public GraphQlMetricsInstrumentation graphQlMetricsInstrumentation(MeterRegistry meterRegistry,
			GraphQlTagsProvider tagsProvider, MetricsProperties properties) {
		return new GraphQlMetricsInstrumentation(meterRegistry, tagsProvider,
				new PropertiesAutoTimer(properties.getGraphql().getAutotime()));
	}

}
