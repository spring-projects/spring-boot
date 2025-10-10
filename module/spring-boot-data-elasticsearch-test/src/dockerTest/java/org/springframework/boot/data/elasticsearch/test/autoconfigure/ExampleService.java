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

package org.springframework.boot.data.elasticsearch.test.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.stereotype.Service;

/**
 * Example service used with {@link DataElasticsearchTest @DataElasticsearchTest} tests.
 *
 * @author Eddú Meléndez
 */
@Service
public class ExampleService {

	private final ElasticsearchTemplate elasticsearchTemplate;

	public ExampleService(ElasticsearchTemplate elasticsearchRestTemplate) {
		this.elasticsearchTemplate = elasticsearchRestTemplate;
	}

	public @Nullable ExampleDocument findById(String id) {
		return this.elasticsearchTemplate.get(id, ExampleDocument.class);
	}

}
