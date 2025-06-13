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

package org.springframework.boot.elasticsearch.autoconfigure.health;

import org.elasticsearch.client.RestClient;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.elasticsearch.health.ElasticsearchRestClientHealthIndicator;
import org.springframework.boot.health.autoconfigure.contributor.CompositeHealthContributorConfiguration;
import org.springframework.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link ElasticsearchRestClientHealthIndicator}.
 *
 * @author Artsiom Yudovin
 * @since 4.0.0
 */
@AutoConfiguration(after = ElasticsearchRestClientAutoConfiguration.class)
@ConditionalOnClass({ RestClient.class, ConditionalOnEnabledHealthIndicator.class })
@ConditionalOnBean(RestClient.class)
@ConditionalOnEnabledHealthIndicator("elasticsearch")
public class ElasticsearchRestHealthContributorAutoConfiguration
		extends CompositeHealthContributorConfiguration<ElasticsearchRestClientHealthIndicator, RestClient> {

	public ElasticsearchRestHealthContributorAutoConfiguration() {
		super(ElasticsearchRestClientHealthIndicator::new);
	}

	@Bean
	@ConditionalOnMissingBean(name = { "elasticsearchHealthIndicator", "elasticsearchHealthContributor" })
	public HealthContributor elasticsearchHealthContributor(ConfigurableListableBeanFactory beanFactory) {
		return createContributor(beanFactory, RestClient.class);
	}

}
