/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.elasticsearch;

import org.elasticsearch.client.Client;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's Elasticsearch
 * support.
 * <p>
 * Registers a {@link ElasticsearchTemplate} if no other bean of the same type is
 * configured.
 * 
 * @author Artur Konczak
 * @author Mohsin Husen
 * @see EnableElasticsearchRepositories
 * @since 1.1.0
 */
@Configuration
@ConditionalOnClass({ Client.class, ElasticsearchTemplate.class })
@AutoConfigureAfter(ElasticsearchAutoConfiguration.class)
public class ElasticsearchDataAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public ElasticsearchTemplate elasticsearchTemplate(Client client) {
		return new ElasticsearchTemplate(client);
	}

}
