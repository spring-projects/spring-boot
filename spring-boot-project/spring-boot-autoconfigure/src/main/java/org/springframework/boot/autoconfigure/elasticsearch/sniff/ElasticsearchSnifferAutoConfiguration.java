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

package org.springframework.boot.autoconfigure.elasticsearch.sniff;

import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.sniff.Sniffer;
import org.elasticsearch.client.sniff.SnifferBuilder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Elasticsearch Sniffer.
 *
 * @since
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(RestHighLevelClient.class)
@ConditionalOnMissingBean(RestClient.class)
@EnableConfigurationProperties(ElasticsearchSnifferProperties.class)
public class ElasticsearchSnifferAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(SnifferBuilder.class)
	static class SnifferBuilderConfiguration {

		@Bean
		SnifferBuilderCustomizer defaultSnifferBuilderCustomizer(ElasticsearchSnifferProperties properties) {
			return new DefaultSnifferBuilderCustomizer(properties);
		}

		@Bean
		Sniffer elasticsearchSnifferBuilder(ElasticsearchSnifferProperties properties,
											RestClient elasticsearchRestClient) {
			return Sniffer.builder(elasticsearchRestClient).build();
		}
	}

	static class DefaultSnifferBuilderCustomizer implements SnifferBuilderCustomizer {

		private static final PropertyMapper map = PropertyMapper.get();

		private final ElasticsearchSnifferProperties properties;

		DefaultSnifferBuilderCustomizer(ElasticsearchSnifferProperties properties) {
			this.properties = properties;
		}

		@Override
		public void customize(SnifferBuilder builder) {
			map.from(this.properties::getSniffIntervalMillis).whenNonNull().asInt(Duration::toMillis)
					.to(builder::setSniffIntervalMillis);
			map.from(this.properties::getSniffFailureDelayMillis).whenNonNull().asInt(Duration::toMillis)
					.to(builder::setSniffAfterFailureDelayMillis);
		}
	}
}
