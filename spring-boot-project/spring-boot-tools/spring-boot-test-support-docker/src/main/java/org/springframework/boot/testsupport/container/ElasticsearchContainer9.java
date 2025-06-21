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

package org.springframework.boot.testsupport.container;

import org.testcontainers.elasticsearch.ElasticsearchContainer;

/**
 * A container suitable for testing Elasticsearch 9.
 *
 * @author Dmytro Nosan
 */
public class ElasticsearchContainer9 extends ElasticsearchContainer {

	public ElasticsearchContainer9() {
		super(TestImage.ELASTICSEARCH_9.toString());
		addEnv("ES_JAVA_OPTS", "-Xms32m -Xmx512m");
		addEnv("xpack.security.enabled", "false");
	}

}
