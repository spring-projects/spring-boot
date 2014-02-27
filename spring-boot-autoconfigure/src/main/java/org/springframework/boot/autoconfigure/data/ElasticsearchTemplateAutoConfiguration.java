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

package org.springframework.boot.autoconfigure.data;

import java.net.UnknownHostException;

import org.elasticsearch.client.Client;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration Auto-configuration} for Spring Data's
 * {@link org.springframework.data.elasticsearch.core.ElasticsearchTemplate}.
 *
 * @author Artur Konczak
 * @author Mohsin Husen
 * @see org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories
 */
@Configuration
@ConditionalOnClass({Client.class, ElasticsearchTemplate.class})
public class ElasticsearchTemplateAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public ElasticsearchTemplate elasticsearchTemplate(Client elasticsearchClient) throws UnknownHostException {
		return new ElasticsearchTemplate(elasticsearchClient);
	}
}
