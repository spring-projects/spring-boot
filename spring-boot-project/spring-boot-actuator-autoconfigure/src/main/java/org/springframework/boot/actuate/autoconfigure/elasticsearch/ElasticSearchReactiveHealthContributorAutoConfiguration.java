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

package org.springframework.boot.actuate.autoconfigure.elasticsearch;

import java.util.Map;

import reactor.core.publisher.Flux;

import org.springframework.boot.actuate.autoconfigure.health.CompositeReactiveHealthContributorConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.elasticsearch.ElasticsearchReactiveHealthIndicator;
import org.springframework.boot.actuate.health.ReactiveHealthContributor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.elasticsearch.ReactiveElasticsearchRestClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link ElasticsearchReactiveHealthIndicator} using the
 * {@link ReactiveElasticsearchClient}.
 *
 * @author Aleksander Lech
 * @since 2.3.2
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ ReactiveElasticsearchClient.class, Flux.class })
@ConditionalOnBean(ReactiveElasticsearchClient.class)
@ConditionalOnEnabledHealthIndicator("elasticsearch")
@AutoConfigureAfter(ReactiveElasticsearchRestClientAutoConfiguration.class)
public class ElasticSearchReactiveHealthContributorAutoConfiguration extends
		CompositeReactiveHealthContributorConfiguration<ElasticsearchReactiveHealthIndicator, ReactiveElasticsearchClient> {

	@Bean
	@ConditionalOnMissingBean(name = { "elasticsearchHealthIndicator", "elasticsearchHealthContributor" })
	public ReactiveHealthContributor elasticsearchHealthContributor(Map<String, ReactiveElasticsearchClient> clients) {
		return createContributor(clients);
	}

}
